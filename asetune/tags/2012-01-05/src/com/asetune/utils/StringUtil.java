/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
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

import org.apache.log4j.Logger;

/**
 * A String utility class.
 *
 * @author Goran Schwarz
 */
public class StringUtil
{
	private static Logger _logger = Logger.getLogger(StringUtil.class);

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
	 */
	public static String word(String str, int number)
	{
		String[] stra = str.split("[ \t\n\f\r]");
		if ( stra.length < number )
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
	public static String left(String str, int num)
	{
		return left(str, num, true);
	}

	/**
	 * Left justify a string and fill extra spaces to the right
	 */
	public static String left(String str, int num, boolean allowGrow)
	{
		if ( allowGrow )
		{
			if ( str.length() > num )
				num = str.length();
		}

		int maxPadSize = num - str.length();
		String space = "                                                                                                                                                                                                                                                               ";
		while (space.length() < maxPadSize)
			space += "                                                                                                                                                                                                                                                               ";

		return (str + space).substring(0, num);
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
		return in.replaceAll("<[^>]+>", "");   // STRIP ALL HTML Tags from the description.
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

		return str.trim().equals("");
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

	/////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////
	//// TEST CODE
	/////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////

	public static void main(String[] args)
	{
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
