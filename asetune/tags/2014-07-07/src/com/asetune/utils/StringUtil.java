/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.JComboBox;

import org.apache.log4j.Logger;

/**
 * A String utility class.
 *
 * @author Goran Schwarz
 */
public class StringUtil
{
	private static Logger _logger = Logger.getLogger(StringUtil.class);

	public static String toCommaStr(boolean[] array) { return Arrays.toString(array).replace("[", "").replace("]", ""); }
	public static String toCommaStr(byte   [] array) { return Arrays.toString(array).replace("[", "").replace("]", ""); }
	public static String toCommaStr(char   [] array) { return Arrays.toString(array).replace("[", "").replace("]", ""); }
	public static String toCommaStr(double [] array) { return Arrays.toString(array).replace("[", "").replace("]", ""); }
	public static String toCommaStr(float  [] array) { return Arrays.toString(array).replace("[", "").replace("]", ""); }
	public static String toCommaStr(int    [] array) { return Arrays.toString(array).replace("[", "").replace("]", ""); }
	public static String toCommaStr(long   [] array) { return Arrays.toString(array).replace("[", "").replace("]", ""); }
//	public static String toCommaStr(Object [] array) { return Arrays.toString(array).replace("[", "").replace("]", ""); }
	public static String toCommaStr(short  [] array) { return Arrays.toString(array).replace("[", "").replace("]", ""); }

	public static <T> String toCommaStr(Collection<T> list)
	{
		return toCommaStr(list, ", ");
	}
	public static <T> String toCommaStr(Collection<T> list, String entrySep)
	{
		if (list == null)
			return "";

		StringBuilder sb = new StringBuilder();
		for (Iterator<T> it = list.iterator(); it.hasNext();)
		{
			T val = it.next();

			sb.append(val);

			if (it.hasNext())
				sb.append(entrySep);
		}
		return sb.toString();
	}

	public static <K,V> String toCommaStr(Map<K,V> map)
	{
		return toCommaStr(map, "=", ", ");
	}
	public static <K,V> String toCommaStrKey(Map<K,V> map)
	{
		return toCommaStr(map, "=", ", ", true, false);
	}
	public static <K,V> String toCommaStrKey(Map<K,V> map, String entrySep)
	{
		return toCommaStr(map, "=", entrySep, true, false);
	}
	public static <K,V> String toCommaStrVal(Map<K,V> map)
	{
		return toCommaStr(map, "=", ", ", false, true);
	}
	public static <K,V> String toCommaStrVal(Map<K,V> map, String entrySep)
	{
		return toCommaStr(map, "=", entrySep, false, true);
	}
	public static <K,V> String toCommaStr(Map<K,V> map, String keyValSep, String entrySep)
	{
		return toCommaStr(map, keyValSep, entrySep, true, true);
	}
	public static <K,V> String toCommaStr(Map<K,V> map, String keyValSep, String entrySep, boolean useKey, boolean useValue)
	{
		if (map == null)
			return "";
		if (map.isEmpty())
			return "";

		StringBuilder sb = new StringBuilder();
		for (Iterator<K> it = map.keySet().iterator(); it.hasNext();)
		{
			K key = it.next();
			V val = map.get(key);

			if (useKey)              sb.append(key);
			if (useKey && useValue)  sb.append(keyValSep);
			if (useValue)            sb.append(val);
//			sb.append(key).append(keyValSep).append(val);
			
			if (it.hasNext())
				sb.append(entrySep);
		}
		return sb.toString();
	}



	public static String toCommaStrMultiMap(Map<String,List<String>> map)
	{
		return toCommaStrMultiMap(map, "=", ", ");
	}
	public static String toCommaStrMultiMapKey(Map<String,List<String>> map)
	{
		return toCommaStrMultiMap(map, "=", ", ", true, false);
	}
	public static String toCommaStrMultiMapKey(Map<String,List<String>> map, String entrySep)
	{
		return toCommaStrMultiMap(map, "=", entrySep, true, false);
	}
	public static String toCommaStrMultiMapVal(Map<String,List<String>> map)
	{
		return toCommaStrMultiMap(map, "=", ", ", false, true);
	}
	public static String toCommaStrMultiMapVal(Map<String,List<String>> map, String entrySep)
	{
		return toCommaStrMultiMap(map, "=", entrySep, false, true);
	}
	public static String toCommaStrMultiMap(Map<String,List<String>> map, String keyValSep, String entrySep)
	{
		return toCommaStrMultiMap(map, keyValSep, entrySep, true, true);
	}
	public static String toCommaStrMultiMap(Map<String,List<String>> map, String keyValSep, String entrySep, boolean useKey, boolean useValue)
	{
		if (map == null)
			return "";
		if (map.isEmpty())
			return "";

		StringBuilder sb = new StringBuilder();
		for (Iterator<String> it = map.keySet().iterator(); it.hasNext();)
		{
			String       key  = it.next();
			List<String> list = map.get(key);

			for (Iterator<String> listIt = list.iterator(); listIt.hasNext();)
			{
				String listVal = listIt.next();
				if (useKey)              sb.append(key);
				if (useKey && useValue)  sb.append(keyValSep);
				if (useValue)            sb.append(listVal);
//				sb.append(key).append(keyValSep).append(listVal);

				if (listIt.hasNext())
					sb.append(entrySep);
			}

			if (it.hasNext())
				sb.append(entrySep);
		}
		return sb.toString();
	}

	/**
	 * Parsers a comma separated list and returns a ArrayList which holds all values.<br>
	 * The entries will be trimmed()...
	 * @param str a comma separated list
	 * @return a ArrayList with Strings
	 */
	public static List<String> parseCommaStrToList(String str)
	{
		ArrayList<String> list = new ArrayList<String>();

		String[] sa = str.split(",");
		for (String se : sa)
			list.add(se.trim());
		
		return list;
	}

	/**
	 * Parsers a comma separated list and returns a Set/LinkedHashSet which holds all values.<br>
	 * The entries will be trimmed()...
	 * @param str a comma separated list
	 * @return a Set with Strings
	 */
	public static Set<String> parseCommaStrToSet(String str)
	{
		LinkedHashSet<String> set = new LinkedHashSet<String>();

		String[] sa = str.split(",");
		for (String se : sa)
			set.add(se.trim());
		
		return set;
	}

	/**
	 * Parse a comma separated key=value string and make it into a Map
	 * <p>
	 * FIXME 1: This implementation is way to simple, it doesnt handle ',' or '=' chars inside the key/value strings etc...
	 * If it finds problems it will just strip the faulty string/chars
	 * FIXME 2: If the "key" value already exists, a warning message will be written to log, is this enough or do we need to do anything else
	 * @param source "aaa=111,bbb=222, ccc=333" or "{aaa=111,bbb=222, ccc=333}"
	 * @return a Map, if nothing was found, the Map will simply be empty. (never returns null)
	 */
	public static Map<String,String> parseCommaStrToMap(String source)
	{
		return parseCommaStrToMap(source, "=", ",");
	}
	public static Map<String,String> parseCommaStrToMap(String source, String keyValSep, String entrySep)
	{
		if (source == null)
			return new LinkedHashMap<String,String>();
		source = source.trim();

		// if string start and ends with {}, strip those chars 
		if (source.startsWith("{") && source.endsWith("}"))
			source = source.substring(1, source.length()-1);

		LinkedHashMap<String,String> map = new LinkedHashMap<String,String>();

		Scanner parser = new Scanner(source);
		parser.useDelimiter(entrySep);
		while (parser.hasNext())
		{
			String keyVal = parser.next().trim();
			String sa[] = split(keyValSep, keyVal);
			if (sa.length == 2)
			{
				String key = sa[0];
				String val = sa[1];

				Object oldVal = map.put(key, val);
				if (oldVal != null)
					_logger.warn("Found an already existing value for the key '"+key+"'. The existing value '"+oldVal+"' is replaced with the new value '"+val+"'.");
			}
		}

		return map;
	}

//	/**
//	 * Parse a comma separated key=value string and make it into a MultiMap<br>
//	 * the MultiMap is just a HashMap, but the value for the key is a linked list
//	 * <p>
//	 * FIXME 1: This implementation is way to simple, it doesnt handle ',' or '=' chars inside the key/value strings etc...
//	 * If it finds problems it will just strip the faulty string/chars
//	 * FIXME 2: use <code>Multimap</code> object instead (see: http://google-collections.googlecode.com/svn/trunk/javadoc/com/google/common/collect/Multimap.html)
//	 * @param source "aaa=111,bbb=222, ccc=333" or "{aaa=111,bbb=222, ccc=333}"
//	 * @return a Map, if nothing was found, the Map will simply be empty. (never returns null)
//	 */
//	public static Map parseCommaStrToMultiMap(String source)
//	{
//		return parseCommaStrToMultiMap(source, "=", ",");
//	}
//	public static Map parseCommaStrToMultiMap(String source, String keyValSep, String entrySep)
//	{
//		if (source == null)
//			return new LinkedHashMap();
//		source = source.trim();
//
//		// if string start and ends with {}, strip those chars 
//		if (source.startsWith("{") && source.endsWith("}"))
//			source = source.substring(1, source.length()-1);
//
//		LinkedHashMap map = new LinkedHashMap();
//
//		Scanner parser = new Scanner(source);
//		parser.useDelimiter(entrySep);
//		while (parser.hasNext())
//		{
//			String keyVal = parser.next().trim();
//			String sa[] = split(keyValSep, keyVal);
//			if (sa.length == 2)
//			{
//				String key = sa[0];
//				String val = sa[1];
//
//				List list = (List) map.get(key);
//				if (list == null)
//					list = new LinkedList();
//
//				// Add the value to the list, then assign the list to the key.
//				list.add(val);
//				map.put(key, list);
//			}
//		}
//
//		return map;
//	}
	/**
	 * Parse a comma separated key=value string and make it into a MultiMap<br>
	 * the MultiMap is just a HashMap, but the value for the key is a linked list
	 * <p>
	 * FIXME 1: This implementation is way to simple, it doesnt handle ',' or '=' chars inside the key/value strings etc...
	 * If it finds problems it will just strip the faulty string/chars
	 * FIXME 2: use <code>Multimap</code> object instead (see: http://google-collections.googlecode.com/svn/trunk/javadoc/com/google/common/collect/Multimap.html)
	 * @param source "aaa=111,bbb=222, ccc=333" or "{aaa=111,bbb=222, ccc=333}"
	 * @return a Map, if nothing was found, the Map will simply be empty. (never returns null)
	 */
	public static Map<String,List<String>> parseCommaStrToMultiMap(String source)
	{
		return parseCommaStrToMultiMap(source, "=", ",");
	}
	public static Map<String,List<String>> parseCommaStrToMultiMap(String source, String keyValSep, String entrySep)
	{
		if (source == null)
			return new LinkedHashMap<String,List<String>>();
		source = source.trim();

		// if string start and ends with {}, strip those chars 
		if (source.startsWith("{") && source.endsWith("}"))
			source = source.substring(1, source.length()-1);

		LinkedHashMap<String,List<String>> map = new LinkedHashMap<String,List<String>>();

		Scanner parser = new Scanner(source);
		parser.useDelimiter(entrySep);
		while (parser.hasNext())
		{
			String keyVal = parser.next().trim();
			String sa[] = split(keyValSep, keyVal);
			if (sa.length == 2)
			{
				String key = sa[0];
				String val = sa[1];

				List<String> list = map.get(key);
				if (list == null)
					list = new LinkedList<String>();

				// Add the value to the list, then assign the list to the key.
				list.add(val);
				map.put(key, list);
			}
		}

		return map;
	}

	public static String toCommaStr(Object[] oa)
	{
		return toCommaStr(oa, ", ");
	}
	public static String toCommaStr(Object[] oa, String sep)
	{
		StringBuffer sb = new StringBuffer();

		if (oa == null)
			return "";

		for(int i=0; i<oa.length; i++)
		{
			sb.append(oa[i]);
			if (i < (oa.length-1))
				sb.append(sep);
		}
		return sb.toString();
	}

	/**
	 * splits every part of a comma separated string into a array element
	 * @param str "db1,db2" returns ["db1"]["db2"].length=2, "db1" returns ["db1"].length=1, "" returns [""].length=1
	 * @return a String[]
	 */
	public static String[] commaStrToArray(String str)
	{
		if (str == null)
			return new String[] {};

		String[] sa = str.split(",");
		for (int i=0; i<sa.length; i++)
			sa[i] = sa[i].trim();

		return sa;
	}

	/**
	 * splits every part of a comma separated string into a LinkedHashSet<String>, since it's a Set duplicates will be removed
	 * @param str "db1,db2,db2" returns LinkedHashSet ["db1"]["db2"]
	 * @return a String[]
	 */
	public static Set<String> commaStrToSet(String str)
	{
		String[] sa = commaStrToArray(str);
		LinkedHashSet<String> retSet = new LinkedHashSet<String>();
		for (String s : sa)
			retSet.add(s);
		return retSet;
	}

	/**
	 * splits every part of a comma separated string into a ArrayList<String>
	 * @param str "db1,db2,db2" returns ArrayList ["db1"]["db2"]["db2"]
	 * @return List<String>
	 */
	public static List<String> commaStrToList(String str)
	{
		String[] sa = commaStrToArray(str);
		ArrayList<String> retList = new ArrayList<String>();
		for (String s : sa)
			retList.add(s);
		return retList;
	}

	/**
	 * 
	 * @param oa
	 * @param o
	 * @return
	 */
	public static boolean arrayContains(Object[] oa, Object o)
	{
		if (oa == null)
			return false;

		for(int i=0; i<oa.length; i++)
		{
			if (oa[i].equals(o))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Counts the number of occurrences of the String <code>occurs</code> in the
	 * String <code>s</code>.
	 *
	 * @param occurs The String to count
	 * @param s The String to count in
	 */
	public static int count(String occurs, String s)
	{
		int count = 0;
		int current = -1;
		int next = 0;
		next = s.indexOf(occurs);
		while ( next > -1)
		{
			count++;
			current = next;
			next = s.indexOf(occurs, current + 1);
		}
		return count;
	}

	/**
	 * Split the String s into an array of strings. The String s is split at
	 * each occurrence of the substring <code>splitAt</code> in <code>s</code>.
	 * <p>
	 * Example:
	 * The string <tt>s = "onextwoxthreexfour"</tt>, with <tt>splitAt = "x"</tt>
	 * is split into an array equivalent to <tt>{"one", "two", "three", "four"}
	 * </tt>.
	 *
	 * @param splitAt Indicates where to split the string s
	 * @param s String to split
	 */
	public static String[] split(String splitAt, String s)
	{
		int noOfLines = count(splitAt, s) + 1;
		java.lang.String[] stringArray = new String[noOfLines];
		String remaining = s;
		int pos;
		int splitLen = splitAt.length();

		for (int i = 0; i < noOfLines - 1; i++)
		{
			pos = remaining.indexOf(splitAt);
			stringArray[i] = remaining.substring(0, pos);
			remaining = remaining.substring(pos + splitLen, remaining.length());
		}

		// last element
		stringArray[noOfLines - 1] = remaining;
		return stringArray;
	}

	/**
	 * get word in string
	 * @param str input string
	 * @param number what word to extract (starts at 0)
	 * @return the exctacted word
	 */
	public static String word(String str, int number)
	{
		String[] stra = str.split("[ \t\n\f\r]");
		if ( stra.length <= number )
		{
			return null;
			// throw new
			// IndexOutOfBoundsException("This string only consists of "+stra.length+", while you want to read word "+number);
		}
		return stra[number];
	}

	/**
     */
	public static String lastWord(String str)
	{
		String[] stra = str.split("[ \t\n\f\r]");
		return stra[stra.length - 1];
	}

	/**
	 * Left justify a string and fill extra spaces to the right
	 */
	public static String left(String str, int expandToSize)
	{
		return left(str, expandToSize, true);
	}

	/**
	 * Left justify a string and fill extra spaces to the right
	 */
	public static String left(String str, int expandToSize, boolean allowGrow)
	{
		return left(str, expandToSize, true, "", "");
	}
	/**
	 * Left justify a string and fill extra spaces to the right
	 */
	public static String left(String str, int expandToSize, boolean allowGrow, String quoteStr)
	{
		return left(str, expandToSize, true, quoteStr, quoteStr);
	}
	/**
	 * Left justify a string and fill extra spaces to the right
	 */
	public static String left(String str, int expandToSize, boolean allowGrow, String quoteStrLeft, String quoteStrRight)
	{
		// max size 128K for a sting to justify, it's big, but it's a limit...
		int maxSize = 128 * 1024;

		// Add left and right quote
		if ( ! isNullOrBlank(quoteStrLeft) )
			str = quoteStrLeft + str;
		if ( ! isNullOrBlank(quoteStrRight) )
			str = str + quoteStrRight;

		if ( allowGrow )
		{
			if ( str.length() > expandToSize )
				expandToSize = str.length();
		}

		if (expandToSize > maxSize)
		{
			_logger.warn("StringUtils.left(): expandToSize can't be above "+maxSize+", using this value. expandToSize="+expandToSize+", inStr.length()="+str.length());
			expandToSize = maxSize;
		}
		int maxPadSize = expandToSize - str.length();
		
		String space = "                                                                                                                                                                                                                                                               ";
		while (space.length() < maxPadSize)
		{
			space += "                                                                                                                                                                                                                                                               ";
		}
		return (str + space).substring(0, expandToSize);
	}

	/**
	 * Right justify a string and fill extra spaces to the left
	 */
	public static String right(String str, int num)
	{
		int len = num - str.length();
		if ( len < 0 )
			len = 0;
		String space = "                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   ";
		return (space.substring(0, len) + str).substring(0, num);
	}

	/**
	 * All ${ENV_VARIABLE_NAME} will be substrituted with the environment
	 * variable from the OperatingSystem. Some JVM's the System.getenv() does
	 * not work<br>
	 * In that case go and get the value from the runtime instead...
	 * 
	 * @param val
	 *            A string that should be substituted
	 * @return The substituted string
	 */
	public static String envVariableSubstitution(String val)
	{
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
					System.out.println("System.getenv(): Is not supported on this platform or version of Java. Please pass '-D"+envName+"=value' when starting the JVM.");
				}
			}
			if (envVal == null)
			{
				System.out.println("The Environment variable '"+envName+"' cant be found, replacing it with an empty string ''.");
				envVal="";
			}
			// Backslashes does not work that good in replaceFirst()...
			// So change them to / instead...
			envVal = envVal.replace('\\', '/');

			// NOW substityte the ENV VARIABLE with a real value...
			val = val.replaceFirst("\\$\\{"+envName+"\\}", envVal);
		}

		return val;
	}


	/**
	 * Duplicate the input string X number of times
	 * @param str
	 * @param size
	 * @return
	 */
	public static String replicate(String str, int size)
	{
		StringBuilder sb = new StringBuilder(size);
		for (int i=0; i<size; i++)
			sb.append(str);
		return sb.toString();
	}

	/**
	 * Add extra space at the end of the input String<br>
	 * If the input string is longer than the fill size, the original string will be returned
	 * 
	 * @param str The string to append spaces on
	 * @param fill The minimum length() of the output String 
	 * @return
	 */
	public static String fill(String str, int fill)
	{
		if (str.length() >= fill)
			return str;

		StringBuilder sb = new StringBuilder(str);

		// Append a long string on every loop
		while (sb.length() < fill)
			sb.append("                                                           ");

		return sb.substring(0, fill);
	}

	/**
	 * For the moment, quite simple just strip of <b>anything</b> that has <> character
	 * 
	 * @param a String with html tags
	 * @return The String without html tags 
	 */
	public static String stripHtml(String in)
	{
		if (in == null)
			return null;

//		return in.replaceAll("\\<.*?\\>", "");   // STRIP ALL HTML Tags from the description. hmmm this stips off '<>' also, which is the same as "not equal" or !=, which is NOT a html tag
//		return in.replaceAll("<[^>]+>", "");   // STRIP ALL HTML Tags from the description.
		return HTML_TAG_PATTERN.matcher(in).replaceAll("");
	}
	private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");

	/**
	 * Remove any trailing white spaces and newlines
	 * @param str
	 * @return
	 */
	public static String stripNewLine(String str)
	{
		if (str == null)
			return null;
		
		if (str.indexOf('\n') >= 0)
			str = str.replace("\n", "");;
		if (str.indexOf('\r') >= 0)
			str = str.replace("\r", "");;
		return str.trim();
	}

	/**
	 * Add newlines to the input string.
	 * @param str Input string
	 * @param rowLength The approx row length
	 * @param wordCountHysteresis If there are only ## words left in the input string.... keep them on the same line
	 * @param lineBreak The string used to make the new line.
	 * @return The in string with line wraps added.
	 */
	public static String makeApproxLineBreak(String str, int rowLength, int wordCountHysteresis, String lineBreak)
	{
		if (str.length() <= rowLength)
			return str;

		// ok chop the string into peaces of about (length) on each row
		StringBuffer sb = new StringBuffer();

		char[] ca = str.toCharArray();
		int c   = 0; // char position in the string
		int car = 0; // Char At Row
		while ( c < ca.length )
		{
			sb.append(ca[c]);
			// if newline was seen, reset the CharAtRow counter
			if (ca[c] == '\n')
				car = 0;
			else
				car++;
			c++; 
			// should we make a line break
			if (car >= rowLength)
			{
				// If next "space" is in reach, break of the line
				// But if it's "out of reach", make a new line at once
				int nextSpacePos = str.substring(c).trim().indexOf(' '); 
				if (nextSpacePos > 0 && nextSpacePos < Math.max(15, rowLength/4))
				{
					// Look ahead to get next "space"
					while ( (c < ca.length) && ca[c] != ' ' )
					{
						sb.append(ca[c]);
						c++;
					}
					
					// Here we could also check if we got more than X words left on the line
					// then we could choose to include them on "this" row, or make a new row for them.
					if (str.substring(c).split("\\s", wordCountHysteresis+1).length <= wordCountHysteresis)
					{
						while ( c < ca.length )
						{
							sb.append(ca[c]);
							c++;
						}
					}
					else
					{
						sb.append(lineBreak);
					}
				}
				else
				{
					sb.append(lineBreak);
				}
				car = 0;
			}
		}
		return sb.toString();
	}

	public static String getHostname()
	{
		try 
		{
			InetAddress addr = InetAddress.getLocalHost();
			
			// Get IP Address
			//byte[] ipAddr = addr.getAddress();

			// Get hostname
			String hostname = addr.getHostName();

			return hostname;
		}
		catch (UnknownHostException e) 
		{
			return null;
		}
	}


	/**
	 * Converts a Throwable to  String
	 * 
	 * @param t A Throwable
	 * @return A String representation of the Throwable
	 */
	public static String stackTraceToString(Throwable t)
	{
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		return sw.toString();
	}

	/**
	 * Checks if a string is null or an empty string "" or a blank string (only spaces in string)
	 * @param str String to check
	 * @return true if null/blank, false if str contains anything else
	 */
	public static boolean isNullOrBlank(String str)
	{
		if (str == null)
			return true;

		if (str.equals(""))
			return true;

		// Strange, this seemed to mess up a Scanner reading each line (removing empty lines at start/end of the string...
		// but Strings should be immutable
//		return str.trim().equals("");

		// OK, lets loop the string until we find any string that is not a whitespace
		int len = str.length();
		for (int i=0; i<len; i++)
		{
//			if ( str.charAt(i) > ' ' ) // trim() does: (char <= ' ') == space, so lets do that to and not the isWhitespace
			if ( ! Character.isWhitespace(str.charAt(i)) )
				return false;
		}
		return true;
	}
	/**
	 * if NOT null or NOT empty, simply do: return ! isNullOrBlank(str)
	 * @param str
	 * @return
	 */
	public static boolean hasValue(String str)
	{
		return ! isNullOrBlank(str);
	}

	/**
	 * Check if 'inStr' is part of the 'regexArr'
	 * @param inStr String to match in any of Strings in the Array
	 * @param regexArr Array of regex Strings that we match towards <code>inStr</code>
	 * @return true if inStr matches any of the regex Strings of the regexArr
	 */
	public static boolean matchesRegexArr(String inStr, String[] regexArr)
	{
		for (String str : regexArr)
		{
			if (inStr.matches(str))
				return true;
		}
		return false;
	}

	/**
	 * Check if 'inStr' is part of the 'regexSet'
	 * @param inStr String to match in any of Strings in the Set
	 * @param regexSet of regex Strings that we match towards <code>inStr</code>
	 * @return true if inStr matches any of the regex Strings of the regexSet
	 */
	public static boolean matchesRegexSet(String inStr, Set<String> regexSet)
	{
		for (String str : regexSet)
		{
			if (inStr.matches(str))
				return true;
		}
		return false;
	}

	/**
	 * Remove last ','
	 * @param str
	 * @return the origin string but last ',' is stripped. 
	 */
	public static String removeLastComma(String str)
	{
		if (str == null)
			return null;

		str = str.trim();

		if (str.endsWith(","))
			str = str.substring(0, str.length()-1);

		return str;
	}
	
	public static String removeLastNewLine(String str)
	{
		if (str == null)
			return null;

		str = str.trim();

		if (str.endsWith("\n"))
			str = str.substring(0, str.length()-1);
		if (str.endsWith("\r"))
			str = str.substring(0, str.length()-1);

		return str;
	}

	/**
	 * Check if last character is a ';'<br>
	 * <pre>
	 * null                     returns false
	 * ""                       returns false
	 * "abc"                    returns false
	 * "abc;"                   returns true
	 * "abc;   "                returns true
	 * "abc;--comment"          returns false
	 * "abc --comment ; "       returns true
	 * "abc --comment ; \t \n"  returns true
	 * </pre>
	 * @param row the string to check
	 * @return if last character is ';'  
	 */
	public static boolean hasSemicolonAtEnd(String row)
	{
		if (StringUtil.isNullOrBlank(row))
			return false;
		
		if (row.endsWith(";"))
			return true;
		
		for (int i=row.length()-1; i>0; i--)
		{
			char ch = row.charAt(i);
			if (Character.isWhitespace(ch))
				continue;
			return ch == ';';
		}
		return false;
	}

	/**
	 * remove everything after last ';', including the ';'<br>
	 * Uses <code>hasSemicolonAtEnd</code> to check if it has a semicolon at the end
	 * <pre>
	 * null                     returns null
	 * ""                       returns ""
	 * "abc"                    returns "abc"
	 * "abc;"                   returns "abc"
	 * "abc;   "                returns "abc"
	 * "abc;--comment"          returns "abc;--comment"
	 * "abc --comment ; "       returns "abc --comment "
	 * "abc --comment ; \t \n"  returns ""abc --comment "
	 * </pre>
	 * @param row the string to check
	 * @return if last character is ';'  
	 */
	public static String removeSemicolonAtEnd(String row)
	{
		if (hasSemicolonAtEnd(row))
			return row.substring(0, row.lastIndexOf(';'));

		return row;
	}

	/**
	 * Count number of matching characters in the string
	 * @param str
	 * @param ch
	 * @return count
	 */
	public static int charCount(String str, char ch)
	{
		int count = 0;
		for (int i=0; i<str.length(); i++)
		{
			if (str.charAt(i) == ch)
				count++;
		}
		return count;
	}

	/**
	 * 
	 * @param str
	 * @return number of rows/lines in the string 1 = one row
	 */
	public static int countLines(String str)
	{
		if (str == null || str.length() == 0)
			return 0;
		int lines = 1;
		int len = str.length();
		for( int pos = 0; pos < len; pos++) 
		{
			char c = str.charAt(pos);
			if( c == '\r' ) 
			{
				lines++;
				if ( pos+1 < len && str.charAt(pos+1) == '\n' )
					pos++;
			} 
			else if( c == '\n' ) 
			{
				lines++;
			}
		}
		return lines;
	}

	/**
	 * Get position of the matching brace.
	 *
	 * @param searchStr the string to search in
	 * @param startPos start position in the serachStr, the position must be <b>after</b> the start char we want to match
	 * @param endChar supported chars are: ), }, ]
	 * 
	 * @return position of matching brace. -1 if it wasn't found.
	 */
	public static int indexOfEndBrace(String searchStr, int startPos, char endChar)
	{
		char beginChar = ' ';

		if      (endChar == '}') beginChar = '{';
		else if (endChar == ']') beginChar = '[';
		else if (endChar == ')') beginChar = '(';

		int matchCount = 1; // When it reaches 0, we exit with the position

		char[] search = searchStr.toCharArray();
		for (int i=startPos; i<search.length; i++)
		{
			char c = search[i];
			if (c == beginChar) matchCount++; // increment if we see any new "begin" char
			if (c == endChar)   matchCount--;
			
			if (matchCount == 0)
				return i;
		}
		return -1;
	}

	/**
	 * Simply do Integer.parseInt(str), but if it fails (NumberFormatException), then return the default value
	 * @param str           String to be converted
	 * @param defaultValue  if "str" can't be converted (NumberFormatException), then return this value
	 * @return a integer value
	 */
	public static int parseInt(String str, int defaultValue)
	{
		try
		{
			return Integer.parseInt(str);
		}
		catch (NumberFormatException nfe)
		{
			return defaultValue;
		}
	}
	/**
	 * returns a "safe" XML tag content string
	 * <p>
	 * if '&' or '<' exists in the passed string a CDATA substitution will be done.
	 * <p>
	 * Example:<br>
	 * <pre>
	 * xmlSafe("abcde") returns "abcd"
	 * xmlSafe("select * from t1 where (c1 & 1024)=1024 and c2 < 99") returns "&lt;![CDATA[select * from t1 where (c1 & 1024)=1024 and c2 < 99]]&gt;"
	 * </pre>
	 *
	 * @param inStr the input string
	 * 
	 * @return a clean XML string that can is escaped if it contains & or < characters.
	 */
	public static String xmlSafe(String inStr)
	{
		if (inStr == null)
			return null;
		
		if (inStr.indexOf('&') >= 0 || inStr.indexOf('<') >= 0)
		{
			return "<![CDATA[" + inStr + "]]>";
		}
		return inStr;
	}

	public static StringBuilder xmlAddAttributes(StringBuilder sb, Map<String, String> attributes)
	{
		if ( attributes == null || (attributes != null && attributes.size() == 0) ) 
			return sb;

		for (String key : attributes.keySet())
		{
			String val = attributes.get(key);
			
			sb.append(" ").append(key).append("=\"").append(val).append("\"");
		}
		return sb;
	}

	private static StringBuilder xmlAddAttributes(StringBuilder sb, String[] attributes)
	{
		if ( attributes == null || (attributes != null && attributes.length == 0) ) 
			return sb;
		if (attributes != null && (attributes.length % 2) != 0)
			throw new IllegalArgumentException("Input parameter attributes must be a multiple of 2: key1, val1, key2, val2");

		for (int i=0; i<attributes.length; i+=2)
		{
			String key = attributes[i];
			String val = attributes[i+1];
			
			sb.append(" ").append(key).append("=\"").append(val).append("\"");
		}
		return sb;
	}

	public static StringBuilder xmlBeginTag(StringBuilder sb, int preSpaces, String tagName)
	{
		return xmlBeginTag(sb, preSpaces, tagName, (String[]) null);
	}
	
	public static StringBuilder xmlBeginTag(StringBuilder sb, int preSpaces, String tagName, Map<String, String> attributes)
	{
		for (int i=0; i<preSpaces; i++)
			sb.append(" ");

		sb.append("<").append(tagName);
		
		if (attributes != null && attributes.size() > 0) 
			xmlAddAttributes(sb, attributes);

		sb.append(">\n");
		return sb;
	}

	public static StringBuilder xmlBeginTag(StringBuilder sb, int preSpaces, String tagName, String... attributes)
	{
		if (attributes != null && (attributes.length % 2) != 0)
			throw new IllegalArgumentException("Input parameter attributes must be a multiple of 2: key1, val1, key2, val2");

		for (int i=0; i<preSpaces; i++)
			sb.append(" ");

		sb.append("<").append(tagName);
		
		if (attributes != null && attributes.length > 0) 
			xmlAddAttributes(sb, attributes);

		sb.append(">\n");
		return sb;
	}

	public static StringBuilder xmlEndTag(StringBuilder sb, int preSpaces, String tagName)
	{
		for (int i=0; i<preSpaces; i++)
			sb.append(" ");
		return sb.append("</").append(tagName).append(">\n");
	}

	public static StringBuilder xmlTag(StringBuilder sb, int preSpaces, String tagName, boolean value)
	{
		return xmlTag(sb, preSpaces, tagName, Boolean.toString(value));
	}
	
	public static StringBuilder xmlTag(StringBuilder sb, int preSpaces, String tagName, int value)
	{
		return xmlTag(sb, preSpaces, tagName, Integer.toString(value));
	}
	
	public static StringBuilder xmlTag(StringBuilder sb, int preSpaces, String tagName, String value)
	{
		return xmlTag(sb, preSpaces, tagName, value, false);
	}
	
	public static StringBuilder xmlTag(StringBuilder sb, int preSpaces, String tagName, String value, boolean addNullOrBlankValues)
	{
		if (StringUtil.isNullOrBlank(value) && !addNullOrBlankValues)
			return sb;

		for (int i=0; i<preSpaces; i++)
			sb.append(" ");

		value = StringUtil.xmlSafe(value);

		if (value == null)
			value = "";

		return sb.append("<").append(tagName).append(">").append(value).append("</").append(tagName).append(">\n");
	}


	/**
	 * if input string has \t, \f, \n or \r they will be escaped into \\t, \\f, \\n, \\r
	 */
	public static String escapeControlChars(String str)
	{
		if (str == null)
			return null;
		
		str = str.replace("\t", "\\t");
		str = str.replace("\f", "\\f");
		str = str.replace("\n", "\\n");
		str = str.replace("\r", "\\r");

		return str;
	}
	/**
	 * if input string has \\t, \\f, \\n or \\r they will be escaped into \t, \f, \n \r
	 */
	public static String unEscapeControlChars(String str)
	{
		if (str == null)
			return null;
		
		str = str.replace("\\t", "\t");
		str = str.replace("\\f", "\f");
		str = str.replace("\\n", "\n");
		str = str.replace("\\r", "\r");

		return str;
	}

	/**
	 * Format a (function or procedure text) or any line of text
	 * 
	 * @param text the object text (if null, null will be returned)
	 * @param line line number to be marked
	 * @param markUsingHtml mark it using HTML font red
	 * @return the objectText with 'line#> text'
	 */
	public static String markTextAtLine(String text, int line, boolean markUsingHtml)
	{
		if (text == null || line <= 0)
			return text;

		StringBuilder sb = new StringBuilder();

		// Walk the lines, add row numbers (and mark the correct line)
		Scanner scanner = new Scanner(text);
		int rowNumber = 0;
		while (scanner.hasNextLine()) 
		{
			rowNumber++;
			String lineStr = scanner.nextLine();

			if (line == rowNumber)
			{
				if (markUsingHtml)
					sb.append("<b><font color=\"red\">").append(rowNumber).append("> ").append(lineStr).append("</font></b>");
				else
					sb.append("**** ").append(rowNumber).append("> ").append(lineStr);
			}
			else
				sb.append(rowNumber).append("> ").append(lineStr);
			
			sb.append("\n");
		}
		return sb.toString();
	}

//	/**
//	 * Emulates String.indexOf(char)<br>
//	 * But takes multiple chars as input, and returns the first position in any of the chars
//	 * 
//	 * @param str the string to search
//	 * @param charsToLookfor the characters to look for
//	 * @return -1 if not found
//	 */
//	public static int indexOf(String str, char... charsToLookfor)
//	{
//		return indexOf(str, 0, charsToLookfor);
//	}

	/**
	 * Emulates String.indexOf(char, fromIndex)<br>
	 * But takes multiple chars as input, and returns the first position in any of the chars
	 * 
	 * @param str
	 * @param fromIndex
	 * @param charsToLookfor
	 * @return
	 */
	public static int indexOf(String str, int fromIndex, char... charsToLookfor)
	{
		if (str == null)
			return -1;

		int len = str.length();
		for (int i=fromIndex; i<len; i++)
		{
			char c = str.charAt(i);
			for (int j=0; j<charsToLookfor.length; j++)
			{
				if (c == charsToLookfor[j])
					return i;
			}
		}
		return -1;
	}

	/**
	 * What line number does the input starts at
	 * 
	 * @param str
	 * @return Line number where input starts. 0 = first line
	 */
	public static int getFirstInputLine(String str)
	{
		if (isNullOrBlank(str))
			return 0;

		int line = 0;
		int len = str.length();
		for (int i=0; i<len; i++)
		{
			char ch = str.charAt(i);

			if (ch == '\r' || ch == '\n')
			{
				line++;

				// If it's a Winows, then treat <CR><NL> as on line-end 
				if (ch == '\r' && (i+1)<len && str.charAt(i+1) == '\n')
					i++;

				continue;
			}

			// skip other whitespaces
			if (Character.isWhitespace(ch))
				continue;
			
			// Other chars, get out of here
			break;
		}
		return line;
	}

	/**
	 * Get a string representation of the selected item in a JComboBox<br>
	 * If nothing is selected a empty string "" will be returned.
	 * 
	 * @param combo the combobox 
	 * @return always a string (toString() on the selected object)
	 */
	public static String getSelectedItemString(JComboBox combo)
	{
		if (combo == null)
			return null;
		
		Object o = combo.getSelectedItem();
		if (o == null)
			return "";
		
		return o.toString();
	}

	/**
	 * Convert a Byte[] to string
	 * @param prefix prefix the string with any value, for example "0x", null means no prefix
	 * @param bytes the byte array
	 * @return a string with the bye value representation
	 */
	public static String bytesToHex(String prefix, byte[] bytes)
	{
		return bytesToHex(prefix, bytes, false, Integer.MAX_VALUE);
	}

	/**
	 * Convert a Byte[] to string
	 * @param prefix prefix the string with any value, for example "0x", null means no prefix
	 * @param bytes the byte array
	 * @param toUpper if you want "abcdef" presented as "ABCDEF"
	 * @return a string with the bye value representation
	 */
	public static String bytesToHex(String prefix, byte[] bytes, boolean toUpper)
	{
		return bytesToHex(prefix, bytes, toUpper, Integer.MAX_VALUE);
	}

	/**
	 * Convert a Byte[] to string
	 * @param prefix  prefix the string with any value, for example "0x", null means no prefix
	 * @param bytes   the byte array
	 * @param toUpper if you want "abcdef" presented as "ABCDEF"
	 * @param max     Maximum number of bytes to translate before stop (max < 0 = Integer.MAX_VALUE)
	 * @return a string with the bye value representation
	 */
	public static String bytesToHex(String prefix, byte[] bytes, boolean toUpper, int max)
	{
		if (bytes == null)
			return null;
		
		if (max < 0)
			max = Integer.MAX_VALUE;

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bytes.length && i < max; i++)
		{
			if (toUpper)
				sb.append(String.format("%02X", bytes[i])); // upper case ABCDEF
			else
				sb.append(String.format("%02x", bytes[i])); // lower case abcdef 
		}
		if (prefix != null)
			sb.insert(0, prefix);

		return sb.toString();
	}

	/////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////
	//// TEST CODE
	/////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////

	public static void main(String[] args)
	{
		
		System.out.println("countLines():" + StringUtil.countLines("one") );
		System.out.println("countLines():" + StringUtil.countLines("1 \n 2 \r\n 3 \r 4 \n 5") );
		System.exit(0);

		System.out.println("TEST 1-1: " + (indexOfEndBrace("abc ( 123 4 5 6 7 ) xxx",   0, ')') == -1 ? "OK" : "FAIL"));
		System.out.println("TEST 1-2: " + (indexOfEndBrace("abc ( 123 4 5 6 7 ) xxx",   5, ')') != -1 ? "OK" : "FAIL"));
		System.out.println("TEST 1-3: " + (indexOfEndBrace("abc ( 123 4 5 6 7 ] xxx",   5, ')') == -1 ? "OK" : "FAIL"));
		System.out.println("TEST 1-4: " + (indexOfEndBrace("abc ( 123 4 5 ( 6 7 ) xxx", 5, ')') == -1 ? "OK" : "FAIL"));
		System.out.println("TEST 1-5: " + (indexOfEndBrace("abc 123 4 5 6 7 ) xxx",     0, ')') != -1 ? "OK" : "FAIL"));
		System.out.println("TEST 1-6: " + (indexOfEndBrace("abc)",                      0, ')') ==  3 ? "OK" : "FAIL"));

		System.exit(0);


		String[] htmlStrArr = {"a<>bc", "<html>a<b>b</b>c</html>", "abc,"};
		for (String s : htmlStrArr)
		{
			System.out.println("str1='"+stripHtml(s)+"'.");
		}
		System.exit(0);

		String[] str = {"abc", "abc ", "abc,", "abc, ", "abc,\t", "abc,\n", "abc,\t\t", "abc,\n\n", "abc,\t\n"};
		for (String s : str)
		{
			System.out.println("str1='"+removeLastComma(s)+"'.");
		}
		System.exit(0);

		String[] strArr = commaStrToArray("");
		System.out.println("length="+strArr.length);
		for (int i=0; i<strArr.length; i++)
		{
			System.out.println("arr["+i+"]='"+strArr[i]+"'.");
		}
		System.exit(0);

		System.out.println("MAP=|" + parseCommaStrToMap("aaa=11\\,11,bbbbb=2222, cccc=3333") +"|.");
		System.out.println("MAP=|" + parseCommaStrToMap("{aaa=1111,bbbbb=2222, cccc=3333}") +"|.");
		System.out.println("MAP=|" + parseCommaStrToMap("{aaa=1111, bbbbb=2222, cccc=3333,}") +"|.");

		String t1Str = "{aaa=1111, bbb=2222, bbb=3333,}";
		Map<String,List<String>> t1Map = parseCommaStrToMultiMap(t1Str);
		System.out.println("MMAP=|" + t1Map +"|.");
		System.out.println("MMAP.toCommaStrMultiMap=|" + toCommaStrMultiMap(t1Map) +"|.");

		System.out.println("MMAP.toCommaStrMultiMap(useKey)=|" + toCommaStrMultiMap(t1Map, "=", ",", true, false) +"|.");
		System.out.println("MMAP.toCommaStrMultiMap(useVal)=|" + toCommaStrMultiMap(t1Map, "=", ",", false, true) +"|.");

		System.out.println("MMAP.toCommaStrMultiMapKey=|" + toCommaStrMultiMapKey(t1Map) +"|.");
		System.out.println("MMAP.toCommaStrMultiMapVal=|" + toCommaStrMultiMapVal(t1Map) +"|.");
//		System.exit(0);

		System.out.println("TEST: StringUtil BEGIN.");

		if ( ! StringUtil.lastWord(" 1 2 3 ").equals("3") )
			System.out.println("FAILED:  test-1: StringUtil.lastWord()");

		if ( ! StringUtil.lastWord("").equals("") )
			System.out.println("FAILED:  test-2: StringUtil.lastWord()");

		if ( ! StringUtil.lastWord(" 1 2\t 3 ").equals("3") )
			System.out.println("FAILED:  test-3: StringUtil.lastWord()");

		if ( ! StringUtil.lastWord(" 1 2 \n3").equals("3") )
			System.out.println("FAILED:  test-4: StringUtil.lastWord()");

		if ( ! StringUtil.left("123", 5).equals("123  ") )
			System.out.println("FAILED:  test-1: StringUtil.left()");

		if ( ! StringUtil.right("  123", 5).equals("  123") )
			System.out.println("FAILED:  test-1: StringUtil.right()");

		if ( ! (StringUtil.fill("123", 1).length() == 3))
			System.out.println("FAILED:  test-1: StringUtil.fill()");

		if ( ! (StringUtil.fill("123", 50).length() == 50))
			System.out.println("FAILED:  test-2: StringUtil.fill(\"123\", 50)");

		if ( ! (StringUtil.fill("123", 5000).length() == 5000))
			System.out.println("FAILED:  test-3: StringUtil.fill(\"123\", 5000)");


		if ( ! (StringUtil.stripHtml("123<html>-<b>B</b>.<xml xx=xxx, z=z>").indexOf("<") == -1) )
			System.out.println("FAILED:  test-1: StringUtil.stripHtml()");

		System.out.println("TEST: StringUtil END.");
	}
}
