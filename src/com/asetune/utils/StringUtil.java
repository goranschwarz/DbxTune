/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.utils;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import javax.swing.JComboBox;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;

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

	/**
	 * Make a quoted string from the input
	 * <p>
	 * Example 1:
	 * <pre>
	 * toCommaStrQuoted("a", "b", "c")
	 * Returns: "a", "b", "c"
	 * </pre>
	 * 
	 * @param names
	 * @return A string...
	 */
	public static String toCommaStrQuoted(Collection<String> collection)
	{
		return toCommaStrQuoted('"', '"', collection);
	}

	/**
	 * Make a quoted string from the input
	 * <p>
	 * Example 1:
	 * <pre>
	 * toCommaStrQuoted('"', "a", "b", "c")
	 * Returns: "a", "b", "c"
	 * </pre>
	 * 
	 * <p>
	 * Example 2:
	 * <pre>
	 * toCommaStrQuoted('|', "a", "b", "c")
	 * Returns: |a|, |b|, |c|
	 * </pre>
	 * 
	 * @param quoteChar chars/str that will be used to suround all the input strings...
	 * @param names
	 * @return A string...
	 */
	public static String toCommaStrQuoted(char quoteChar, Collection<String> collection)
	{
		return toCommaStrQuoted(quoteChar, quoteChar, collection);
	}

	/**
	 * Make a quoted string from the input
	 * <p>
	 * Example 1:
	 * <pre>
	 * toCommaStrQuoted('"', '"', "a", "b", "c")
	 * Returns: "a", "b", "c"
	 * </pre>
	 * 
	 * <p>
	 * Example 2:
	 * <pre>
	 * toCommaStrQuoted('<', '>', "a", "b", "c")
	 * Returns: &lt;a&gt;, &lt;b&gt;, &lt;c&gt;
	 * </pre>
	 * 
	 * @param startQuoteChar chars/str that will be used to start-suround all the input strings...
	 * @param endQuoteChar chars/str that will be used to end-suround all the input strings...
	 * @param names
	 * @return A string...
	 */
	public static String toCommaStrQuoted(char startQuoteChar, char endQuoteChar, Collection<String> collection)
	{
		if (collection        == null) return "";
		if (collection.size() ==    0) return "";
		
		StringBuilder sb = new StringBuilder();

		for (String name : collection)
		{
			sb.append(startQuoteChar).append(name).append(endQuoteChar).append(", ");
		}
		// Remove last ", "
		sb.delete(sb.length()-2, sb.length());

		return sb.toString();
	}

	/**
	 * Make a quoted string from the input
	 * <p>
	 * Example 1:
	 * <pre>
	 * toCommaStrQuoted("a", "b", "c")
	 * Returns: "a", "b", "c"
	 * </pre>
	 * 
	 * @param names
	 * @return A string...
	 */
	public static String toCommaStrQuoted(String...names)
	{
		return toCommaStrQuoted('"', '"', names);
	}

	/**
	 * Make a quoted string from the input
	 * <p>
	 * Example 1:
	 * <pre>
	 * toCommaStrQuoted('"', "a", "b", "c")
	 * Returns: "a", "b", "c"
	 * </pre>
	 * 
	 * <p>
	 * Example 2:
	 * <pre>
	 * toCommaStrQuoted('|', "a", "b", "c")
	 * Returns: |a|, |b|, |c|
	 * </pre>
	 * 
	 * @param quoteChar chars/str that will be used to suround all the input strings...
	 * @param names
	 * @return A string...
	 */
	public static String toCommaStrQuoted(char quoteChar, String...names)
	{
		return toCommaStrQuoted(quoteChar, quoteChar, names);
	}

	/**
	 * Make a quoted string from the input
	 * <p>
	 * Example 1:
	 * <pre>
	 * toCommaStrQuoted('"', '"', "a", "b", "c")
	 * Returns: "a", "b", "c"
	 * </pre>
	 * 
	 * <p>
	 * Example 2:
	 * <pre>
	 * toCommaStrQuoted('<', '>', "a", "b", "c")
	 * Returns: &lt;a&gt;, &lt;b&gt;, &lt;c&gt;
	 * </pre>
	 * 
	 * @param startQuoteChar chars/str that will be used to start-suround all the input strings...
	 * @param endQuoteChar chars/str that will be used to end-suround all the input strings...
	 * @param names
	 * @return A string...
	 */
	public static String toCommaStrQuoted(char startQuoteChar, char endQuoteChar, String...names)
	{
		if (names        == null) return "";
		if (names.length ==    0) return "";
		
		StringBuilder sb = new StringBuilder();

		for (String name : names)
		{
			sb.append(startQuoteChar).append(name).append(endQuoteChar).append(", ");
		}
		// Remove last ", "
		sb.delete(sb.length()-2, sb.length());

		return sb.toString();
	}




	/**
	 * Make a quoted string from the input
	 * <p>
	 * Example 1:
	 * <pre>
	 * toCommaStrQuoted("\"", "a", "b", "c")
	 * Returns: "a", "b", "c"
	 * </pre>
	 * 
	 * <p>
	 * Example 2:
	 * <pre>
	 * toCommaStrQuoted("|", "a", "b", "c")
	 * Returns: |a|, |b|, |c|
	 * </pre>
	 * 
	 * @param quoteChar chars/str that will be used to suround all the input strings...
	 * @param names
	 * @return A string...
	 */
	public static String toCommaStrQuoted(String quoteChar, Collection<String> collection)
	{
		return toCommaStrQuoted(quoteChar, quoteChar, collection);
	}

	/**
	 * Make a quoted string from the input
	 * <p>
	 * Example 1:
	 * <pre>
	 * toCommaStrQuoted("\"", "\"", "a", "b", "c")
	 * Returns: "a", "b", "c"
	 * </pre>
	 * 
	 * <p>
	 * Example 2:
	 * <pre>
	 * toCommaStrQuoted("<", ">", "a", "b", "c")
	 * Returns: &lt;a&gt;, &lt;b&gt;, &lt;c&gt;
	 * </pre>
	 * 
	 * @param startQuoteChar chars/str that will be used to start-suround all the input strings...
	 * @param endQuoteChar chars/str that will be used to end-suround all the input strings...
	 * @param names
	 * @return A string...
	 */
	public static String toCommaStrQuoted(String startQuoteChar, String endQuoteChar, Collection<String> collection)
	{
		if (collection        == null) return "";
		if (collection.size() ==    0) return "";
		
		StringBuilder sb = new StringBuilder();

		for (String name : collection)
		{
			sb.append(startQuoteChar).append(name).append(endQuoteChar).append(", ");
		}
		// Remove last ", "
		sb.delete(sb.length()-2, sb.length());

		return sb.toString();
	}

	/**
	 * Make a quoted string from the input
	 * <p>
	 * Example 1:
	 * <pre>
	 * toCommaStrQuoted("\"", "a", "b", "c")
	 * Returns: "a", "b", "c"
	 * </pre>
	 * 
	 * <p>
	 * Example 2:
	 * <pre>
	 * toCommaStrQuoted("|", "a", "b", "c")
	 * Returns: |a|, |b|, |c|
	 * </pre>
	 * 
	 * @param quoteChar chars/str that will be used to suround all the input strings...
	 * @param names
	 * @return A string...
	 */
	public static String toCommaStrQuoted(String quoteChar, String...names)
	{
		return toCommaStrQuoted(quoteChar, quoteChar, names);
	}

	/**
	 * Make a quoted string from the input
	 * <p>
	 * Example 1:
	 * <pre>
	 * toCommaStrQuoted("\"", "\"", "a", "b", "c")
	 * Returns: "a", "b", "c"
	 * </pre>
	 * 
	 * <p>
	 * Example 2:
	 * <pre>
	 * toCommaStrQuoted("<", ">", "a", "b", "c")
	 * Returns: &lt;a&gt;, &lt;b&gt;, &lt;c&gt;
	 * </pre>
	 * 
	 * @param startQuoteChar chars/str that will be used to start-suround all the input strings...
	 * @param endQuoteChar chars/str that will be used to end-suround all the input strings...
	 * @param names
	 * @return A string...
	 */
	public static String toCommaStrQuoted(String startQuoteChar, String endQuoteChar, String...names)
	{
		if (names        == null) return "";
		if (names.length == 0)    return "";

		StringBuilder sb = new StringBuilder();

		for (String name : names)
		{
			sb.append(startQuoteChar).append(name).append(endQuoteChar).append(", ");
		}
		// Remove last ", "
		sb.delete(sb.length()-2, sb.length());

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
	 * Finds all entries in the list that matches the regex
	 * 
	 * @param  list    The list of strings to check
	 * @param  regex   The regular expression to use
	 * 
	 * @return list containing the entries of all matching entries, if none found it's an empty list
	 */
	public static List<String> getMatchingStrings(List<String> list, String regex) 
	{
		ArrayList<String> matches = new ArrayList<>();

		Pattern p = Pattern.compile(regex);

		for (String str : list) 
		{
			if (p.matcher(str).matches()) 
			{
				matches.add(str);
			}
		}

		return matches;
	}

	
	/**
	 * Parsers a comma separated list and returns a ArrayList which holds all values.<br>
	 * The entries will be trimmed()...
	 * @param str a comma separated list
	 * @return a ArrayList with Strings  (if input string is null, return empty list)
	 */
	public static List<String> parseCommaStrToList(String str)
	{
		ArrayList<String> list = new ArrayList<String>();

		if (str == null)
			return list;

		String[] sa = str.split(",");
		for (String se : sa)
			list.add(se.trim());
		
		return list;
	}

	/**
	 * Parsers a comma separated list and returns a Set/LinkedHashSet which holds all values.<br>
	 * The entries will be trimmed()...<br>
	 * Empty entries will be removed<br>
	 * 
	 * @param str a comma separated list
	 * @return a Set with Strings
	 */
//	public static Set<String> parseCommaStrToSet(String str)
	public static LinkedHashSet<String> parseCommaStrToSet(String str)
	{
		return parseCommaStrToSet(str, true);
	}

	/**
	 * Parsers a comma separated list and returns a Set/LinkedHashSet which holds all values.<br>
	 * The entries will be trimmed()...<br>
	 * Empty entries will be removed<br>
	 * 
	 * @param str                a comma separated list
	 * @param skipEmptyFields    Should empty fields be removed or added to the set
	 * @return a Set with Strings
	 */
//	public static Set<String> parseCommaStrToSet(String str, boolean skipEmptyFields)
	public static LinkedHashSet<String> parseCommaStrToSet(String str, boolean skipEmptyFields)
	{
		LinkedHashSet<String> set = new LinkedHashSet<String>();

		if (str == null)
			return set;

		String[] sa = str.split(",");
		for (String se : sa)
		{
			String trimmed = se.trim();
			if (StringUtil.hasValue(trimmed))
				set.add(trimmed);
		}
		
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
//			String keyVal = parser.next().trim();
//			String sa[] = split(keyValSep, keyVal);
//			if (sa.length == 2)
//			{
//				String key = sa[0];
//				String val = sa[1];
//
//				Object oldVal = map.put(key, val);
//				if (oldVal != null)
//					_logger.warn("Found an already existing value for the key '"+key+"'. The existing value '"+oldVal+"' is replaced with the new value '"+val+"'.");
//			}
			String keyVal = parser.next().trim();

			// NOTE: if the value part contains "keyValSep" character, the  following will NOT work: String sa[] = split(keyValSep, keyVal);
			//       instead take everything after "keyValSep" character
			String key = keyVal;
			String val = "";
			int keyValSepPos = keyVal.indexOf(keyValSep);
			if (keyValSepPos >= 0)
			{
				key = keyVal.substring(0, keyValSepPos);
				val = keyVal.substring(keyValSepPos+1);
			}

			Object oldVal = map.put(key, val);
			if (oldVal != null)
				_logger.warn("Found an already existing value for the key '"+key+"'. The existing value '"+oldVal+"' is replaced with the new value '"+val+"'.");
		}
		parser.close();

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
		parser.close();

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
	 * splits every part of a comma separated string into a array element
	 * @param str                "db1,db2" returns ["db1"]["db2"].length=2, "db1" returns ["db1"].length=1
	 * @param skipEmtyStrings    do not add empty strings to the output list
	 * @return a String[]
	 */
	public static String[] commaStrToArray(String str, boolean skipEmtyStrings)
	{
		if (str == null)
			return new String[] {};

		int emptyCount = 0;
		
		String[] sa = str.split(",");
		for (int i=0; i<sa.length; i++)
		{
			sa[i] = sa[i].trim();
			if (sa[i].equals("") && skipEmtyStrings)
				emptyCount++;
		}

		// Remove empty strings: Create new Array... copy only records with non-blank content
		if (emptyCount > 0)
		{
			String[] tmp = new String[ sa.length - emptyCount ];
			
			for (int i=0,t=0; i<sa.length; i++)
			{
				if (sa[i].equals("") && skipEmtyStrings)
					continue;
				
				tmp[t++] = sa[i];
			}
			sa = tmp;
		}
		
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
	 * splits every part of a comma separated string into a LinkedHashSet<String>, since it's a Set duplicates will be removed
	 * @param str                "db1,db2,db2" returns LinkedHashSet ["db1"]["db2"]
	 * @param skipEmtyStrings    do not add empty strings to the output list
	 * @return a Set<String>
	 */
	public static Set<String> commaStrToSet(String str, boolean skipEmtyStrings)
	{
		String[] sa = commaStrToArray(str);
		LinkedHashSet<String> retSet = new LinkedHashSet<String>();
		for (String s : sa)
		{
			if (s.equals("") && skipEmtyStrings)
				continue;
			retSet.add(s);
		}
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
	 * splits every part of a comma separated string into a ArrayList<String>
	 * @param str                "db1,db2,db2" returns ArrayList ["db1"]["db2"]["db2"]
	 * @param skipEmtyStrings    do not add empty strings to the output list
	 * @return List<String>
	 */
	public static List<String> commaStrToList(String str, boolean skipEmtyStrings)
	{
		String[] sa = commaStrToArray(str);
		ArrayList<String> retList = new ArrayList<String>();
		for (String s : sa)
		{
			if (s.equals("") && skipEmtyStrings)
				continue;
			retList.add(s);
		}
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
	 * Check if "any" of the strings is equal to
	 * 
	 * @param strToCheck Sting to check
	 * @param equalsAny  if strToChack is equal to any of the strings here...
	 * 
	 * @return true if any match, otherwise False
	 */
	public static boolean equalsAny(String strToCheck, String... equalsAny)
	{
		for (String str : equalsAny)
		{
			if (strToCheck.equals(str))
				return true;
		}
		return false;
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
	 * Left justify a string and fill extra spaces to the right, with allowGrow
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
	public static String left(String str, int expandToSize, String quoteStr)
	{
		return left(str, expandToSize, true, quoteStr, quoteStr);
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
		if (str == null)
			str = "";

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
	 * Substitutes environment variables within a string by values.
	 * <p>
	 * This class takes a piece of text and substitutes all the variables within it.
	 * The default definition of a variable is <code>${variableName}</code>.
	 * <p>
	 * Variable values are resolved from system properties.<br>
	 * Note: use </code>setEnvironmentVariablesToSystemProperties()</code> to transfer envVars 
	 * to system properties when the application is started. 
	 * <p>
	 * Example of how to use this method:
	 * <pre>
	 * StringUtils.envVariableSubstitution("You are running with java.version = ${java.version} and os.name = ${os.name}.");
	 * </pre>
	 * <p>
	 * 
	 * Also, this method allows to set a default value for unresolved variables.
	 * The default value for a variable can be appended to the variable name after the variable
	 * default value delimiter. The default value of the variable default value delimiter is ':-',
	 * as in bash and other *nix shells.
	 * <pre>
	 * String templateString = &quot;Java Version ${java.version}, DUMMY_NAME=${DUMMY_NAME:-123}.&quot;;
	 * String resolvedString = StringUtils.envVariableSubstitution(templateString);
	 * </pre>
	 * yielding:
	 * <pre>
	 *      Java Version 1.8.0_65, DUMMY_NAME=123.
	 * </pre>
	 * <p>
	 */
	public static String envVariableSubstitution(String str)
	{
		// NOTE: Deprecated. as of 3.6, use commons-text StrSubstitutor instead
		//       but that requres java 1.8, so switch this in next release instead
		return StrSubstitutor.replaceSystemProperties(str);
	}

	/** 
	 * Works like {@link #envVariableSubstitution}, but instead of System Properties, take values from the valuesMap
	 * 
	 * @param templateString
	 * @param valuesMap
	 * @return String with changed values.
	 * @see envVariableSubstitution 
	 */
	public static String variableSubstitution(String templateString, Map<String, String> valuesMap)
	{
		// NOTE: Deprecated. as of 3.6, use commons-text StrSubstitutor instead
		//       but that requres java 1.8, so switch this in next release instead
		StrSubstitutor sub = new StrSubstitutor(valuesMap);
		return sub.replace(templateString);
	}

//	/**
//	 * All ${ENV_VARIABLE_NAME} will be substituted with the environment <br>
//	 * variable from the OperatingSystem. Some JVM's the System.getenv() does not work <br>
//	 * In that case go and get the value from the runtime instead... <br>
//	 *  <br>
//	 *  TODO: implement default value like ${ENV_VARIABLE_NAME:-defaultValue} and or ${ENV_VARIABLE_NAME:=defaultValue}<br>
//	 *   
//	 * @param val   A string that should be substituted
//	 * @return The substituted string
//	 */
//	public static String envVariableSubstitution(String val)
//	{
//		// Extract Environment variables
//		// search for ${ENV_NAME}
//		Pattern compiledRegex = Pattern.compile("\\$\\{.*\\}"); // or maybe: "\\$\\{[A-Za-z0-9_]+\\}"
//		while( compiledRegex.matcher(val).find() )
//		{
//			String envVal  = null;
//			String envName = val.substring( val.indexOf("${")+2, val.indexOf("}") );
//
//			// Get value for a specific env variable
//			// But some java runtimes does not do getenv(),
//			// then we need to revert back to getProperty() from the system property
//			// then the user needs to pass that as a argument -Dxxx=yyy to the JVM
//			try
//			{
//				envVal  = System.getenv(envName);
//			}
//			catch (Throwable t)
//			{
//				envVal = System.getProperty(envName);
//				if (envVal == null)
//				{
//					_logger.warn("System.getenv(): Is not supported on this platform or version of Java. Please pass '-D"+envName+"=value' when starting the JVM.");
//					System.out.println("System.getenv(): Is not supported on this platform or version of Java. Please pass '-D"+envName+"=value' when starting the JVM.");
//				}
//			}
//
//			// If we can't find it at the ENV fall back and get it from System.getProperty()
//			if (envVal == null)
//			{
//				envVal = System.getProperty(envName);
//			}
//
//			// Not found at all, simply set it to "" -- empty
//			if (envVal == null)
//			{
//				_logger.warn("The Environment variable '"+envName+"' cant be found, replacing it with an empty string ''.");
//				System.out.println("The Environment variable '"+envName+"' cant be found, replacing it with an empty string ''.");
//				envVal="";
//			}
//			// Backslashes does not work that good in replaceFirst()...
//			// So change them to / instead...
//			envVal = envVal.replace('\\', '/');
//
//			// NOW substitute the ENV VARIABLE with a real value...
//			val = val.replaceFirst("\\$\\{"+envName+"\\}", envVal);
//		}
//
//		return val;
//	}

	/**
	 * Get value for a System Property or a Environment Variable<br>
	 * First check System Properties, then check Environment Variable
	 * 
	 * @param envName Name of the environment variable or Property
	 * @param systemPropOverridesEnvVar TRUE = if you first want to check System.getProperty(envName)
	 * @return The value, null if it can't be found.
	 */
	public static String getEnvVariableValue(String envName)
	{
		return getEnvVariableValue(envName, true);
	}

	/**
	 * Get value for a System Property or a Environment Variable
	 * 
	 * @param envName Name of the environment variable or Property
	 * @param systemPropOverridesEnvVar TRUE = if you first want to check System.getProperty(envName)
	 * @return The value, null if it can't be found.
	 */
	public static String getEnvVariableValue(String envName, boolean systemPropOverridesEnvVar)
	{
		if (envName == null)
			return null;

		String envVal  = null;

		// First check the SystemProperty
		if (systemPropOverridesEnvVar)
		{
    		envVal = System.getProperty(envName);
    		if (envVal != null)
    			return envVal;
		}

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
				System.out.println("System.getenv(): Is not supported on this platform or version of Java. Please pass '-D"+envName+"=value' when starting the JVM.");
			}
		}

		// If we can't find it at the ENV fall back and get it from System.getProperty()
		if (envVal == null)
		{
			envVal = System.getProperty(envName);
		}

		// Not found at all, simply set it to "" -- empty
		if (envVal == null)
		{
			_logger.debug("The Environment variable '"+envName+"' cant be found.");
		}

		return envVal;
	}

	/**
	 * Take all Environment variables and add them as System Properties
	 * 
	 * @param overwrite                      overwrite the system properties
	 * @param writeInfoOnOverwrittenValues   if overwrite=true, also write informational message if we overwrite a System Property
	 */
	public static void setEnvironmentVariablesToSystemProperties(boolean overwrite, boolean writeInfoOnOverwrittenValues)
	{
		Map<String, String> envVars = System.getenv();
		for (String key : envVars.keySet())
		{
			String val = envVars.get(key);

			if (overwrite)
			{
				String oldVal = System.setProperty(key, val);
				
				if (writeInfoOnOverwrittenValues && oldVal != null && !oldVal.equals(val))
				{
					_logger.info("Overwriting environment variable '"+key+"'. oldValue='"+oldVal+"', newValue='"+val+"'");
				}
			}
			else
			{
				String oldVal = System.getProperty(key, null);
				
				// Only write if we did NOT have a value
				if (oldVal == null)
				{
					System.setProperty(key, val);
				}
			}

		}
	}

	/**
	 * Count how long (how many bytes) this String will take in-case it's translated to UTF-8
	 * @param sequence
	 * @return
	 */
	// Below was grabbed from: https://stackoverflow.com/questions/8511490/calculating-length-in-utf-8-of-java-string-without-actually-encoding-it
	public static int utf8Length(CharSequence str) 
	{
		if (str == null)
			return 0;

		int count = 0;
		for (int i = 0, len = str.length(); i < len; i++) 
		{
			char ch = str.charAt(i);

			if (ch <= 0x7F) {
				count++;
			} else if (ch <= 0x7FF) {
				count += 2;
			} else if (Character.isHighSurrogate(ch)) {
				count += 4;
				++i;
			} else {
				count += 3;
			}
		}
		return count;
	}

	/**
	 * Truncate end of string, if the string would contain MORE than the allowed number of BYTES for UTF-8 storage.
	 * 
	 * @param sequence
	 * @param maxBytes
	 * @return
	 */
	public static String utf8Truncate(CharSequence str, int maxBytes) 
	{
		if (str == null)
			return null;

		StringBuilder sb = new StringBuilder();
		
		int count = 0;
		for (int i = 0, len = str.length(); i < len; i++) 
		{
			char ch = str.charAt(i);
			
			if (ch <= 0x7F) {
				count++;
			} else if (ch <= 0x7FF) {
				count += 2;
			} else if (Character.isHighSurrogate(ch)) {
				count += 4;
				++i;
			} else {
				count += 3;
			}
			
			// Exit early when we reaches maxBytes
			if (count > maxBytes)
				return sb.toString();
			
			sb.append(ch);
		}
		return sb.toString();
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

	public static String fillSpace(int spaceCnt)
	{
		StringBuilder sb = new StringBuilder(spaceCnt);
		for (int i=0; i<spaceCnt; i++)
			sb.append(' ');

		return sb.toString();
	}

	/**
	 * Simply remove start/end tag of the string: so "&lt;html&gt;12345&lt;/html&gt" returns "12345"
	 * @param str
	 * @return
	 */
	public static String stripHtmlStartEnd(String str)
	{
		if (str == null)
			return null;
		
		if (str.startsWith("<html>") || str.startsWith("<HTML>") )
			str = str.substring("<html>".length());

		if (str.endsWith("</html>") || str.endsWith("</HTML>") )
			str = str.substring(0, str.length() - "</html>".length());
		
		return str;
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
	 * For the moment, quite simple just a regex "<[^>]*>" to check 
	 * 
	 * @param a String to check for html tags
	 * @return true if any html tags is available 
	 */
	public static boolean containsHtml(String in)
	{
		if (in == null)
			return false;

		return HTML_TAG_PATTERN.matcher(in).find();
	}
	
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

	/** Get host name in the short form <code>host1</code> */
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

	/** Get host name in the long form <code>host1.acme.com</code> */
	public static String getHostnameWithDomain()
	{
		try 
		{
			InetAddress addr = InetAddress.getLocalHost();
			
			// Get IP Address
			//byte[] ipAddr = addr.getAddress();

			// Get hostname
			String hostname = addr.getCanonicalHostName();

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
	 * same as isNullOrBlank() but for many fields
	 * @param str String(s) to check
	 * @return true if null/blank, false if str(s) contains anything else
	 */
	public static boolean isNullOrBlankForAll(String... inputs)
	{
		if (inputs == null)
			return true;
		
		for (String str : inputs)
		{
			if ( ! isNullOrBlank(str) )
				return false;
		}
		return true;
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
	 * same as hasValue() but for many fields
	 * @param str
	 * @return
	 */
	public static boolean hasValueForAll(String... inputs)
	{
		if (inputs == null)
			return false;
		
		for (String str : inputs)
		{
			if ( isNullOrBlank(str) )
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
	 * Remove last "endStr"
	 * @param str
	 * @param endStr
	 * @return the origin string but if str end with "endStr" 
	 */
	public static String removeLastStr(String str, String endStr)
	{
		if (str == null)
			return null;

		str = rtrim(str);

		if (str.endsWith(endStr))
			str = str.substring(0, str.length()-endStr.length());

		return str;
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

//		str = str.trim();
		str = rtrim(str);

		if (str.endsWith(","))
			str = str.substring(0, str.length()-1);

		return str;
	}
	
	public static String removeLastNewLine(String str)
	{
		if (str == null)
			return null;

//		str = str.trim();
		str = rtrim2(str);

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
		
		for (int i=row.length()-1; i>=0; i--)
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
	 * Simply do Integer.parseInt(str), but if it fails (NumberFormatException or input is null), then return the default value
	 * @param str           String to be converted
	 * @param defaultValue  if "str" can't be converted (NumberFormatException or input is null), then return this value
	 * @return a integer value
	 */
	public static int parseInt(String str, int defaultValue)
	{
		if (str == null)
			return defaultValue;
//			throw new NullPointerException("parseInt(str) expects a string value not a null");

		try
		{
			return Integer.parseInt(str.trim());
		}
		catch (NumberFormatException nfe)
		{
			return defaultValue;
		}
	}

	/**
	 * Simply do Long.parseLong(str), but if it fails (NumberFormatException), then return the default value
	 * @param str           String to be converted
	 * @param defaultValue  if "str" can't be converted (NumberFormatException), then return this value
	 * @return a long value
	 */
	public static long parseLong(String str, long defaultValue)
	{
		try
		{
			return Long.parseLong(str);
		}
		catch (NumberFormatException nfe)
		{
			return defaultValue;
		}
	}

	/**
	 * returns a "safe" SQL String for column or table names
	 * <p>
	 * This is simple for the moment, and will just do [colname]
	 * 
	 * @param inStr the input string
	 * 
	 * @return "[inStr]"
	 */
	public static String sqlSafeAlways(String inStr)
	{
		return sqlSafe(inStr, "[", "]", null, true);
	}
	/**
	 * returns a "safe" SQL String for column or table names
	 * 
	 * @param inStr    the input string
	 * @param preStr   the string to put before the input str
	 * @param postStr  the string to put after the input str
	 * @param reservedWords  A list of words that we should "save" the string with...
	 * @param always   If you always want to "safe" the string or not.
	 * 
	 * @return preStr + inStr + postStr     or  just the inStr
	 */
	public static String sqlSafe(String inStr, String preStr, String postStr, List<String> reservedWords, boolean always)
	{
		if (isNullOrBlank(inStr))
			return inStr;

		boolean doQuote = false;
		if (always)
		{
			doQuote = true;
		}
		else
		{
			if (reservedWords != null && reservedWords.contains(inStr))
				doQuote = true;

			// Check for strange characters
			//if (strangeChars)
			//	doQuote = true;
		}
		
		if (doQuote)
		{
			StringBuilder sb = new StringBuilder();
			return sb.append(preStr).append(inStr).append(postStr).toString();
		}
		return inStr;
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
	 * Check if this "could" be a XML document...<br>
	 * First check if the string starts with '<?xml ' or '<?XML ', then it is a xml documemt...<br>
	 * Loop until we find: '<'...'>' and a closing tag '</' (max first 255 chars)<br>
	 * A '<...>' must be present before a '</' closing tag... then it might be some form of XML document.
	 * 
	 * @param xmlInString The string to examen
	 * @return
	 */
	public static boolean isPossibleXml(String xmlInString)
	{
		if (StringUtil.isNullOrBlank(xmlInString))
			return false;

		if (xmlInString.startsWith("<?xml "))
			return true;
		
		if (xmlInString.startsWith("<?XML "))
			return true;
		
		int posLt    = -1; // found '<'
		int posGt    = -1; // found '>'
		int posClose = -1; // found '</'
		
		posLt    = xmlInString.indexOf('<');
		posGt    = xmlInString.indexOf('>');
		posClose = xmlInString.indexOf("</");
		// Loop first 255 characters
		int len = Math.min(255, xmlInString.length());

		// Loop until we find: Bracket Quote and Colon
		// Then we can decide it it's possible a JSON
		// Break if we find any nonWhiteSpaces before the first '{' or '['
		for (int i=0; i<len; i++)
		{
			final char ch  = xmlInString.charAt(i);
			final char ch2 = (i+1 < len) ? xmlInString.charAt(i+1) : ' ';

			if ( posLt < 0 && ch == '<')
				posLt = i;
			
			if ( posGt < 0 && ch == '>' )
				posGt = i;
			
			if ( posClose < 0 && ch == '<' && ch2 == '/')
				posClose = i;

			// If we have found Bracket Quote and Colon
			if (posLt >= 0 && posGt >= 0 && posClose >= 0)
			{
				return posLt < posGt 
					&& posLt < posClose;
			}
		}
		return false;
	}

//	public static String xmlFormat(String xml)
//	{
//		// If the first chars in XML starts with strange chars... remove them... 
//		String firstChars = xml.substring(0, 40).toLowerCase();
//		int xmlStart = firstChars.indexOf("<?xml");
//		if (xmlStart > 0)
//			xml = xml.substring(xmlStart);
//
//		try
//		{
//			final InputSource src = new InputSource(new StringReader(xml));
//			final Node document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(src).getDocumentElement();
//			final Boolean keepDeclaration = Boolean.valueOf(xml.startsWith("<?xml"));
//
//			// May need this: System.setProperty(DOMImplementationRegistry.PROPERTY,"com.sun.org.apache.xerces.internal.dom.DOMImplementationSourceImpl");
//
//			final DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
//			final DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("LS");
//			final LSSerializer writer = impl.createLSSerializer();
//
//			writer.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE); // Set this to true if the output needs to be beautified.
//			writer.getDomConfig().setParameter("xml-declaration", keepDeclaration); // Set this to true if the declaration is needed to be in the output.
//
//			return writer.writeToString(document);
//		}
//		catch (Exception e)
//		{
//			throw new RuntimeException(e);
//		}
//		try
//		{
//			final InputSource src = new InputSource(new StringReader(xml));
//			final Node document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(src).getDocumentElement();
//
//			// the last parameter sets indenting/pretty-printing to true:
//			OutputFormat outputFormat = new OutputFormat("WHATEVER", "UTF-8", true);
//			// line width = 0 means no line wrapping:
//			outputFormat.setLineWidth(0);
//			StringWriter sw = new StringWriter();
//			XML11Serializer writer = new XML11Serializer(sw, outputFormat);
//			writer.serialize((Element) document);
//			return sw.toString();
//		}
//		catch (Exception e)
//		{
//			throw new RuntimeException(e);
//		}
//	}
	
	/**
	 * Try to format the XML...
	 * 
	 * @param xml
	 * @return
	 */
	public static String xmlFormat(String xml)
	{
		if (xml == null)
			return null;

		int len = Math.min(40, xml.length());
		
		// If the first chars in XML starts with strange chars... remove them... 
		String firstChars = xml.substring(0, len).toLowerCase();
		int xmlStart = firstChars.indexOf("<?xml");
		if (xmlStart > 0)
			xml = xml.substring(xmlStart);
		
//		return prettyFormat1(xml, 2);
		return prettyFormat2(xml);
	}

	// https://www.journaldev.com/71/java-xml-formatter-document-xml
	private static String prettyFormat1(String inputStr, int indentSize)
	{
		Source xmlInput = new StreamSource(new StringReader(inputStr));
		StringWriter stringWriter = new StringWriter();
		try
		{
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//			transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "yes");
//			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", String.valueOf(indentSize));
			transformer.transform(xmlInput, new StreamResult(stringWriter));

			String retStr = stringWriter.toString().trim();
			return retStr;
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}	
	private static String prettyFormat2(String inputStr)
	{
		try
		{
			final Boolean keepDeclaration = Boolean.valueOf(inputStr.startsWith("<?xml"));
			
			final InputSource src = new InputSource(new StringReader(inputStr));
			final Node document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(src).getDocumentElement();

			final DOMImplementationLS dom =(DOMImplementationLS) DOMImplementationRegistry.newInstance().getDOMImplementation("LS");
			final LSSerializer serializer = dom.createLSSerializer();
			serializer.setNewLine("\n");
			serializer.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE); // Set this to true if the output needs to be beautified.
			serializer.getDomConfig().setParameter("xml-declaration", keepDeclaration); // Set this to true if the declaration is needed to be in the output.
			final LSOutput destination = dom.createLSOutput();
			destination.setEncoding(StandardCharsets.UTF_8.name());

			final ByteArrayOutputStream bos = new ByteArrayOutputStream();
			destination.setByteStream(bos);
			serializer.write(document, destination);
			
			//return bos.toString();
			return bos.toString(StandardCharsets.UTF_8.name());
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
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
	 * @param line line number to be marked (if -1 nothing will be done, if 0 only row prefix will be added)
	 * @param markUsingHtml mark it using HTML font red
	 * @return the objectText with 'line#> text'
	 */
	public static String markTextAtLine(String text, int line, boolean markUsingHtml)
	{
		if (text == null || line < 0)
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
		scanner.close();
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
	@SuppressWarnings("rawtypes")
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

	/**
	 * Convert a hexadecimal string into bytes (if the String starts with 0x it will be removed)
	 * @param hexStr
	 * @return
	 */
	public static byte[] hexToBytes(String hexStr)
	{
		if (hexStr.startsWith("0x"))
			hexStr = hexStr.substring(2);

		int len = hexStr.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) 
		{
			data[i / 2] = (byte) ((Character.digit(hexStr.charAt(i), 16) << 4) + Character.digit(hexStr.charAt(i+1), 16));
		}
		return data;		
	}

	/**
	 * Split on commas (,) but if a commas is found inside quotes (" or ') then treat that comma to be part of the string
	 * <p>
	 * It allows both single(') and double quotes(") to start/terminate a string.<br>
	 * Example:<br>
	 * <pre>
	 *      splitOnCommasAllowQuotes("a, b, c, 'd,d,d', 'It''s a str', \"e,f,g\", \"now that's \"\"strange\"\".\"");
	 *      returns 7 elements: [a][b][c]['d,d,d']['It''s a str']["e,f,g"]["now that's ""strange""."]
	 * </pre> 
	 * to unquote a string see {@link #unquote(String str)}
	 * <p>
	 * 
	 * @param input a comma separated String
	 * @param trim if you want any leading/trailing blanks before/after the commas to be trimmed away.
	 * @return A List of Strings 
	 * 
	 */
	public static List<String> splitOnCommasAllowQuotes(String input, boolean trim)
	{
		return splitOnCharAllowQuotes(input, ',', trim, false, false);
	}

	/**
	 * Split on commas (,) but if a commas is found inside quotes (" or ') then treat that comma to be part of the string
	 * <p>
	 * It allows both single(') and double quotes(") to start/terminate a string.<br>
	 * Example:<br>
	 * <pre>
	 *      splitOnCommasAllowQuotes("a, b, c, 'd,d,d', 'It''s a str', \"e,f,g\", \"now that's \"\"strange\"\".\"");
	 *      returns 7 elements: [a][b][c]['d,d,d']['It''s a str']["e,f,g"]["now that's ""strange""."]
	 * </pre> 
	 * to unquote a string see {@link #unquote(String str)}
	 * <p>
	 * 
	 * @param input a comma separated String
	 * @param trim if you want any leading/trailing blanks before/after the commas to be trimmed away.
	 * @param unquote If you want to unquote strings using {@link #unquote(String str)}
	 * @return A List of Strings 
	 * 
	 */
	public static List<String> splitOnCommasAllowQuotes(String input, boolean trim, boolean unquote)
	{
		return splitOnCharAllowQuotes(input, ',', trim, unquote, false);
	}

	/**
	 * Split on input:splitChar, but if the splitChar is found inside quotes (" or ') then treat that splitChar to be part of the string
	 * <p>
	 * It allows both single(') and double quotes(") to start/terminate a string.<br>
	 * Example:<br>
	 * <pre>
	 *      splitOnCharAllowQuotes("a, b, c, 'd,d,d', 'It''s a str', \"e,f,g\", \"now that's \"\"strange\"\".\"", ',');
	 *      returns 7 elements: [a][b][c]['d,d,d']['It''s a str']["e,f,g"]["now that's ""strange""."]
	 * </pre> 
	 * to unquote a string see {@link #unquote(String str)}
	 * <p>
	 * 
	 * @param input     a String that should be split
	 * @param splitChar The character to split the string on
	 * @param trim      if you want any leading/trailing blanks before/after the commas to be trimmed away.
	 * @return A List of Strings 
	 * 
	 */
	public static List<String> splitOnCharAllowQuotes(String input, char splitChar, boolean trim)
	{
		return splitOnCharAllowQuotes(input, splitChar, trim, false, false);
	}
	/**
	 * Split on input:splitChar, but if the splitChar is found inside quotes (" or ') then treat that splitChar to be part of the string
	 * <p>
	 * It allows both single(') and double quotes(") to start/terminate a string.<br>
	 * Example:<br>
	 * <pre>
	 *      splitOnCharAllowQuotes("a, b, c, 'd,d,d', 'It''s a str', \"e,f,g\", \"now that's \"\"strange\"\".\"", ',');
	 *      returns 7 elements: [a][b][c]['d,d,d']['It''s a str']["e,f,g"]["now that's ""strange""."]
	 * </pre> 
	 * to unquote a string see {@link #unquote(String str)}
	 * <p>
	 * 
	 * @param input     a String that should be split
	 * @param splitChar The character to split the string on
	 * @param trim      if you want any leading/trailing blanks before/after the commas to be trimmed away.
	 * @param unquote If you want to unquote strings using {@link #unquote(String str)}
	 * @return A List of Strings 
	 * 
	 */
	public static List<String> splitOnCharAllowQuotes(String input, char splitChar, boolean trim, boolean unquote, boolean removeEmty)
	{
		List<String> tokensList = new ArrayList<String>();
		if (input == null)
			return tokensList;

		char startQuoteChar = 0;
		boolean inQuotes = false;
		StringBuilder b = new StringBuilder();

		char[] ca = input.toCharArray();
		char c, pc, nc;
		for (int i=0; i<ca.length; i++)
		{
			c = ca[i];
			pc = i==0 ? 0 : ca[i-1]; // previous char
			nc = i+1 < ca.length ? ca[i+1] : 0; // next char

			if (c == '"' || c == '\'')
			{
				if ( ! inQuotes )
				{
					inQuotes = true;
					startQuoteChar = c;
				}
				else
				{
					// handle empty strings: str='',str2=''
					if (pc == startQuoteChar)
						inQuotes = false;

					// handle empty strings: 'it''s true 2sq'''''
					if (c == startQuoteChar && nc != startQuoteChar && pc != startQuoteChar)
						inQuotes = false;
				}
				
				b.append(c);
			}
			else if (c == splitChar)
			{
				if ( inQuotes )
				{
					b.append(c);
				}
				else
				{
					String str = b.toString();
					if (trim)
						str = str.trim();
					if (unquote)
						str = unquote(str);

					boolean addVal = true;
					if (removeEmty && isNullOrBlank(str))
						addVal = false;
					
					if (addVal)
						tokensList.add( str );

					b = new StringBuilder();
				}
			}
			else
			{
				b.append(c);
			}
		}
		String str = b.toString();
		if (trim)
			str = str.trim();
		if (unquote)
			str = unquote(str);

		boolean addVal = true;
		if (removeEmty && isNullOrBlank(str))
			addVal = false;
		
		if (addVal)
			tokensList.add( str );

		return tokensList;
	}

	/**
	 * Remove any leading trailing (and embedded) quotes from a string
	 * <p>
	 * <pre>
	 *       'd,d,d'                   => d,d,d
	 *       'It''s a str'             => It's a str
	 *       "now that's ""strange""." => now that's "strange".
	 *       none quoted str           => none quoted str
	 * </pre>
	 * @param str The string to unquote
	 * @return the unquoted string
	 */
	public static String unquote(String str)
	{
		if (str == null)           return null;
		if (str.length() == 0)     return str;

		// If first char isn't: single-quote or double-quote, just return
		str = str.trim();
		if (str.charAt(0) != '\'' && str.charAt(0) != '"') 
			return str; 

		StringBuilder b = new StringBuilder();

		char[] ca = str.toCharArray();
		char startQuoteChar = ca[0];
		char c, pc;
		boolean justSkippedDoubleQuote = false;
		for (int i=1; i<ca.length-1; i++) // start 1 char in, stop 1 char from end
		{
			c  = ca[i];
			pc = i<=1 ? 0 : ca[i-1]; // prev char (on first loop pc is 0)

			if (c == startQuoteChar && pc == startQuoteChar && !justSkippedDoubleQuote)
			{
				justSkippedDoubleQuote = true;
				continue;
			}
			justSkippedDoubleQuote = false;
			
			b.append(c);
		}

		
		return b.toString();
	}

	/**
	 * Crack a command line.
	 *
	 * @param toProcess    the command line to process
	 * @param removeFirst  If you want to remove the first word (which may be the application name)
	 * @return the command line broken into strings. An empty or null toProcess parameter results in a zero sized array
	 *         
	 * NOTE: code was borrowed from Apache Commons Library, but it was made private...
	 */
//	public static String[] translateCommandline(final String toProcess) 
//	{
//		return translateCommandline(toProcess, true);
//	}
	public static String[] translateCommandline(final String toProcess, boolean removeFirst) 
	{
		if (toProcess == null || toProcess.length() == 0) 
		{
			// no command? no string
			return new String[0];
		}

		// parse with a simple finite state machine
		final int normal = 0;
		final int inQuote = 1;
		final int inDoubleQuote = 2;
		int state = normal;
		final StringTokenizer tok = new StringTokenizer(toProcess, "\"\' ", true);
		final ArrayList<String> list = new ArrayList<String>();
		StringBuilder current = new StringBuilder();
		boolean lastTokenHasBeenQuoted = false;

		while (tok.hasMoreTokens()) 
		{
			final String nextTok = tok.nextToken();

			switch (state) 
			{
			case inQuote:
				if ("\'".equals(nextTok)) 
				{
					lastTokenHasBeenQuoted = true;
					state = normal;
				} 
				else 
				{
					current.append(nextTok);
				}
				break;

			case inDoubleQuote:
				if ("\"".equals(nextTok)) 
				{
					lastTokenHasBeenQuoted = true;
					state = normal;
				} 
				else 
				{
					current.append(nextTok);
				}
				break;

			default:
				if ("\'".equals(nextTok)) 
				{
					state = inQuote;
				} 
				else if ("\"".equals(nextTok)) 
				{
					state = inDoubleQuote;
				} 
				else if (" ".equals(nextTok)) 
				{
					if (lastTokenHasBeenQuoted || current.length() != 0) 
					{
						list.add(current.toString());
						current = new StringBuilder();
					}
				} 
				else 
				{
					current.append(nextTok);
				}
				lastTokenHasBeenQuoted = false;
				break;
			}
		}

		if (lastTokenHasBeenQuoted || current.length() != 0) 
		{
			list.add(current.toString());
		}

		if (state == inQuote || state == inDoubleQuote) 
		{
			throw new IllegalArgumentException("Unbalanced quotes in " + toProcess);
		}
		
		if (removeFirst && ! list.isEmpty())
		{
			list.remove(0);
		}

		final String[] args = new String[list.size()];
		return list.toArray(args);
	}

	public static String bytesToHuman(long size)
	{
		return bytesToHuman(size, null);
	}
	public static String bytesToHuman(long size, String fmt)
	{
		if (fmt == null)
			fmt = "#.##";
		
		long Kb = 1  * 1024;
		long Mb = Kb * 1024;
		long Gb = Mb * 1024;
		long Tb = Gb * 1024;
		long Pb = Tb * 1024;
		long Eb = Pb * 1024;

		if (size <  Kb)                 return new DecimalFormat(fmt).format(        size     ) + " byte";
		if (size >= Kb && size < Mb)    return new DecimalFormat(fmt).format((double)size / Kb) + " KB";
		if (size >= Mb && size < Gb)    return new DecimalFormat(fmt).format((double)size / Mb) + " MB";
		if (size >= Gb && size < Tb)    return new DecimalFormat(fmt).format((double)size / Gb) + " GB";
		if (size >= Tb && size < Pb)    return new DecimalFormat(fmt).format((double)size / Tb) + " TB";
		if (size >= Pb && size < Eb)    return new DecimalFormat(fmt).format((double)size / Pb) + " PB";
		if (size >= Eb)                 return new DecimalFormat(fmt).format((double)size / Eb) + " EB";

		return "???";
	}

	/**
	 * null save way to call <code>trim()</code> on a string...<br>
	 * Simply call <code>return s.trim()</code> on the string
	 * 
	 * @param s the string to trim
	 * @return if null, returns null, otherwise the trimed string by calling <code>s.trim()</code>
	 */
	public static String trim(String s)
	{
		if (s == null)
			return s;

		return s.trim();
	}

	public static String ltrim(String s) 
	{
		if (s == null)
			return s;
		
	    int i = 0;
	    while (i < s.length() && Character.isWhitespace(s.charAt(i))) 
	        i++;

	    return s.substring(i);
	}

	/**
	 * Remove trailing spaces (including newlines etc, it uses Character.isWhitespace()
	 * @param s
	 * @return
	 */
	public static String rtrim(String s) 
	{
		if (s == null)
			return s;
		
	    int i = s.length()-1;
	    while (i >= 0 && Character.isWhitespace(s.charAt(i))) 
	        i--;

	    return s.substring(0,i+1);
	}

	/**
	 * Remove trailing spaces, removes trailing characters (' ', '\t', '\f')
	 * @param s
	 * @return
	 */
	public static String rtrim2(String s) 
	{
		if (s == null)
			return s;
		
	    int i = s.length()-1;
	    while (i >= 0 && ( s.charAt(i) == ' ' || s.charAt(i) == '\t' || s.charAt(i) == '\f' ) ) 
	        i--;

	    return s.substring(0,i+1);
	}

    /**
     * as str.indexOf("someStr") but ignore string case sensitivity.
     *
	 * @param haystack The String to check if needle is part of
	 * @param needle   The String to find in the haystack
     * @return  if the string argument occurs as a substring within this
     *          object, then the index of the first character of the first
     *          such substring is returned; if it does not occur as a
     *          substring, <code>-1</code> is returned.
	 * 
	 * @return
	 */
	// NOTE: grabbed from http://stackoverflow.com/questions/1126227/indexof-case-sensitive
	public static int indexOfIgnoreCase(final String haystack, final String needle)
	{
		if ( needle.isEmpty() || haystack.isEmpty() )
		{
			// Fallback to legacy behavior.
			return haystack.indexOf(needle);
		}

		for (int i = 0; i < haystack.length(); ++i)
		{
			// Early out, if possible.
			if ( i + needle.length() > haystack.length() )
			{
				return -1;
			}

			// Attempt to match substring starting at position i of haystack.
			int j = 0;
			int ii = i;
			while (ii < haystack.length() && j < needle.length())
			{
				char c = Character.toLowerCase(haystack.charAt(ii));
				char c2 = Character.toLowerCase(needle.charAt(j));
				if ( c != c2 )
				{
					break;
				}
				j++;
				ii++;
			}
			// Walked all the way to the end of the needle, return the start
			// position that this was found.
			if ( j == needle.length() )
			{
				return i;
			}
		}

		return -1;
	}

	/**
	 * Check if the list contains a string (case insensitive)
	 * 
	 * @param list
	 * @param find
	 * @return
	 */
	public static boolean containsIgnoreCase(final List<String> list, final String find)
	{
		for (String str : list)
		{
			if (str.equalsIgnoreCase(find))
				return true;
		}
		return false;
	}
	
	/**
	 * Get index of a list (case insensitive)
	 * 
	 * @param list
	 * @param find
	 * @return -1 if NOT found, else the position in the list
	 */
	public static int indexOfIgnoreCase(final List<String> list, final String find)
	{
		int index = 0;
		for (String str : list)
		{
			if (str.equalsIgnoreCase(find))
				return index;
			index++;
		}
		return -1;
	}
	
	
	/**
	 * Transform a string into a string according to RFC 4180<br>
	 * see: <a href="http://tools.ietf.org/html/rfc4180">http://tools.ietf.org/html/rfc4180</a>
	 * @param data a String value
	 * @return
	 */
	public static String toRfc4180String(String str) 
	{
		if (str == null)
			return null;

		boolean hasNewline = str.indexOf('\n') >= 0 || str.indexOf('\r') >= 0;
		boolean hasQuote   = str.indexOf('"')  >= 0;
		boolean hasComma   = str.indexOf(',')  >= 0;

		if (hasNewline || hasQuote || hasComma)
		{
			if (hasQuote)
				str = str.replace("\"", "\"\"");
			str = "\"" + str + "\"";
		}
		return str;
	}

	
	/** 
	 *  Translates:
	 *  <ul>
	 *    <li>'&' to '&amp;amp;'   </li>
	 *    <li>'<' to '&amp;lt;'    </li>
	 *    <li>'>' to '&amp;gt;'    </li>
	 *    <li>'\n' to '&lt;br&gt;' </li>
	 *  </ul>
	 */
	public static String toHtmlString(Object o)
	{
		if (o == null)
			return null;

		String str = o.toString();
		str = str.replace("&", "&amp;");
		str = str.replace("<", "&lt;");
		str = str.replace(">", "&gt;");
		str = str.replace("\\n", "<br>");

		return str;
	}
	
	/** 
	 * Same as toHtmlString() but do NOT translate '\n' newline into '&lt;br&gt;'
	 * <p>
	 *  Translates:
	 *  <ul>
	 *    <li>'&' to '&amp;amp;'   </li>
	 *    <li>'<' to '&amp;lt;'    </li>
	 *    <li>'>' to '&amp;gt;'    </li>
	 *  </ul>
	 */
	public static String toHtmlStringExceptNl(Object o)
	{
		if (o == null)
			return null;

		String str = o.toString();
		str = str.replace("&", "&amp;");
		str = str.replace("<", "&lt;");
		str = str.replace(">", "&gt;");

		return str;
	}
	
	/**
	 * Produce a 2 column HTML Table from a <code>Map</code> with NO column headers
	 * 
	 * @param map            Map with <b>COLUMN-NAME</b>  and COLUMN-VALUE
	 * @return
	 */
	public static String toHtmlTable(Map<String, Object> map)
	{
		return toHtmlTable(map, (String[]) null);
	}

	/**
	 * Produce a 2 column HTML Table from a <code>Map</code>
	 * 
	 * @param map            Map with <b>COLUMN-NAME</b>  and COLUMN-VALUE
	 * @param colNames       If we want to have any column names
	 * @return
	 */
	public static String toHtmlTable(Map<String, Object> map, String... colNames)
	{
		if (map == null)
			return "";

		StringBuilder sb = new StringBuilder();

		// START tag
		sb.append("<table> \n");

		// THREAD
		if (colNames != null && colNames.length > 0)
		{
			sb.append("<thead> \n");
			sb.append("  <tr> \n");
			for (String colName : colNames)
				sb.append("    <th nowrap>").append( colName ).append("</th> \n");
			sb.append("  </tr> \n");
			sb.append("</thead> \n");
		}

		// TBODY
		sb.append("<tbody> \n");
		for (Entry<String, Object> entry : map.entrySet())
		{
			sb.append("  <tr> \n");
			sb.append("    <td nowrap><b>").append( entry.getKey()   ).append("</b></td> \n");
			sb.append("    <td nowrap>")   .append( entry.getValue() ).append("</td> \n");
			sb.append("  </tr> \n");
		}
		sb.append("</tbody> \n");

		// END tag
		sb.append("</table> \n");
		
		return sb.toString();
	}

	
	
	
	public static String toTableString(List<String> tHead, List<List<Object>> tData)
	{
		return toTableString(tHead, tData, "<SQL-NULL>", false, null, null, -1, -1, null);
	}
	public static String toTableString(List<String> tHead, List<List<Object>> tData, String nullValue)
	{
		return toTableString(tHead, tData, nullValue, false, null, null, -1, -1, null);
	}
	public static String toTableString(List<String> tHead, List<List<Object>> tData, String nullValue, int[] justRowNumbers)
	{
		int firstRow = justRowNumbers[0];
		int lastRow  = justRowNumbers[justRowNumbers.length-1] + 1;
		return toTableString(tHead, tData, nullValue, false, null, null, firstRow, lastRow, justRowNumbers);
	}
	public static String toTableString(List<String> tHead, List<List<Object>> tData, String nullValue, int justRowNumber)
	{
		return toTableString(tHead, tData, nullValue, false, null, null, justRowNumber, justRowNumber+1, null);
	}
	public static String toTableString(List<String> tHead, List<List<Object>> tData, String nullValue, boolean stripHtml, String[] prefixColName, Object[] prefixColData)
	{
		return toTableString(tHead, tData, nullValue, stripHtml, prefixColName, prefixColData, -1, -1, null);
	}
	/**
	 * Turn a <List <List<Object> > into a String table
	 * 
	 * @param model a JTable's TableModel
	 * @param stripHtml If cell value contains html tags, remove them...
	 * @param prefixColName Optional Column Names to be added as "prefix" columns
	 * @param prefixColData Optional Column Data to be added as "prefix" columns, the value will be repeated for every row
	 * @param firstRow Starting row number in the table. If this is -1, it means start from row 0
	 * @param lastRow Last row number in the table. If this is -1, it means 'to the end of the table'
	 * 
	 * @return a String in a table format
	 * <p>
	 * <pre>
	 * +-------+-------+-------+----------+
	 * |SPID   |KPID   |BatchID|LineNumber|
	 * +-------+-------+-------+----------+
	 * |38     |2687017|2      |1         |
	 * |38     |2687017|2      |1         |
	 * +-------+-------+-------+----------+
	 * Rows 2
	 * </pre>
	 */
	private static String REGEXP_NEW_LINE = "\\r?\\n|\\r";

	public static String toTableString(List<String> tHead, List<List<Object>> tData, String nullValue, boolean stripHtml, String[] prefixColName, Object[] prefixColData, int firstRow, int lastRow, int[] justRowNumbers)
	{
		//------------------------------------------------------------------------------------------------------------------
		// NOTE: the code is copied from SwingUtilites.tableToString()... so it might look a bit strange in some places.
		//       hopefully later the SwingUtilites.tableToString() will call this code... just so we don't have duplicate code...
		//------------------------------------------------------------------------------------------------------------------

		String colSepOther = "+";
		String colSepData  = "|";
		String lineSpace   = "-";
		String newLine     = "\n";

		// first copy the information to Array list
		// This was simples to do if we want to add pre/post columns...
		ArrayList<String>       tableHead = new ArrayList<String>();
		ArrayList<List<Object>> tableData = new ArrayList<List<Object>>();

		StringBuilder sb = new StringBuilder();

		boolean doPrefix = false;
		if (prefixColName != null && prefixColData != null)
		{
			if (prefixColName.length != prefixColData.length)
				throw new IllegalArgumentException("tableToString(): prefixColName.length="+prefixColName.length+" is NOT equal prefixColData.length="+prefixColData.length);
			doPrefix = true;
		}

		if (tHead == null)
		{
			int cols = tData.get(0).size();
			tHead = new ArrayList<String>();

			for (int c=0; c<cols; c++)
				tableHead.add("dummy-"+(c+1));
			
		}

		int cols = tHead.size();
//		boolean useAllRows = (firstRow < 0) && (lastRow  < 0);
		
		if (firstRow < 0) firstRow = 0;
		if (lastRow  < 0) lastRow = tData.size();
		int copiedRows = 0;

		//------------------------------------
		// Copy COL NAMES
		//------------------------------------
		if (doPrefix)
			for (int c=0; c<prefixColName.length; c++)
				tableHead.add(prefixColName[c]);

		for (int c=0; c<cols; c++)
			tableHead.add(tHead.get(c));

		//------------------------------------
		// Copy ROWS (from firstRow to lastRow)
		//------------------------------------
		for (int r=firstRow; r<lastRow; r++)
		{
			if (justRowNumbers != null)
			{
				boolean addThisRow = false;
				for (int a=0; a<justRowNumbers.length; a++)
				{
					if ( r == justRowNumbers[a] )
					{
						addThisRow = true;
						break;
					}
				}
				if ( ! addThisRow )
					continue;
			}

			ArrayList<Object> row = new ArrayList<Object>();
			if (doPrefix)
				for (int c=0; c<prefixColData.length; c++)
					row.add(prefixColData[c]);

			List<Object> dataRow = tData.get(r);
			for (int c=0; c<cols; c++)
			{
//				Object obj = jtable.getValueAt(r, c);
				Object obj = dataRow.get(c);

				// Strip of '\n' at the end of Strings
				if (obj != null && obj instanceof String)
				{
					String str = (String) obj;
					
					// strip off HTML chars
					if (stripHtml)
						str = StringUtil.stripHtml(str);

					// if the string ENDS with a newline, remove it
					while (str.endsWith("\r") || str.endsWith("\n"))
						str = str.substring(0, str.length()-1);

					// replace all tab's with 8 spaces
					if (str.indexOf('\t') >= 0)
						str = str.replace("\t", "        ");

					// if we have a "multiple row/line cell"
					if (str.indexOf('\r') >= 0 || str.indexOf('\n') >= 0)
						obj = str.split(REGEXP_NEW_LINE); // object "type" would be String[]
					else
						obj = str;
				}
				
				if (obj == null)
					obj = nullValue;

				row.add(obj);
			}
			
			tableData.add(row);
			copiedRows++;
		}

		// Add prefixColCount to cols
		if (doPrefix)
			cols += prefixColName.length;

		//------------------------------------
		// Get MAX column length and store in colLength[]
		// Get MAX newLines/numberOfRows in each cell...
		//------------------------------------
		boolean tableHasMultiLineCells = false;
		int[]   colLength           = new int[cols];
		int[][] rowColCellLineCount = new int[copiedRows][cols];
//		int[]   rowMaxLineCount     = new int[copiedRows];
		for (int c=0; c<cols; c++)
		{
			int maxLen = 0;

			// ColNames
			String cellName = tableHead.get(c);
			maxLen = Math.max(maxLen, cellName.length());
			
			// All the rows, for this column
			for (int r=0; r<copiedRows; r++)
			{
				Object cellObj = tableData.get(r).get(c);
				String cellStr = cellObj == null ? "" : cellObj.toString();

				// Set number of "rows" within the cell
				rowColCellLineCount[r][c] = 0;
				if (cellObj instanceof String[])
				{
					String[]sa = (String[]) cellObj;
					tableHasMultiLineCells = true;

					rowColCellLineCount[r][c] = sa.length;

					for (int l=0; l<sa.length; l++)
						maxLen = Math.max(maxLen, sa[l].length());
				}
				else
				{
					maxLen = Math.max(maxLen, cellStr.length());
				}
			}
			
			colLength[c] = maxLen;
		}


		//-------------------------------------------
		// Print the TABLE HEAD
		//-------------------------------------------
		// +------+------+-----+\n
		for (int c=0; c<cols; c++)
		{
			String line = StringUtil.replicate(lineSpace, colLength[c]);
			sb.append(colSepOther).append(line);
		}
		sb.append(colSepOther).append(newLine);

		// |col1|col2   |col3|\n
		for (int c=0; c<cols; c++)
		{
			String cellName = tableHead.get(c);
			String data = StringUtil.fill(cellName, colLength[c]);
			sb.append(colSepData).append(data);
		}
		sb.append(colSepData).append(newLine);

		// +------+------+-----+\n
		for (int c=0; c<cols; c++)
		{
			String line = StringUtil.replicate(lineSpace, colLength[c]);
			sb.append(colSepOther).append(line);
		}
		sb.append(colSepOther).append(newLine);

		
		//-------------------------------------------
		// Print the TABLE DATA
		//-------------------------------------------
		for (int r=0; r<copiedRows; r++)
		{
			// First loop cols on this row and check for any multiple lines in any o the cells
			int maxCellLineCountOnThisRow = 0;
			for (int c=0; c<cols; c++)
			{
				maxCellLineCountOnThisRow = Math.max(maxCellLineCountOnThisRow, rowColCellLineCount[r][c]);
			}

			// Add a extra "row" separator if any cells has multiple lines
			if (tableHasMultiLineCells && r > 0)
			{
				// +------+------+-----+\n
				for (int c=0; c<cols; c++)
				{
					String line = StringUtil.replicate(lineSpace, colLength[c]);
					sb.append(colSepOther).append(line);
				}
				sb.append(colSepOther).append(newLine);
			}

			// NO multiple lines for any cells on this row
			if (maxCellLineCountOnThisRow == 0)
			{
				// |col1|col2   |col3|\n
				for (int c=0; c<cols; c++)
				{
					Object cellObj = tableData.get(r).get(c);
					String cellStr = cellObj == null ? "" : cellObj.toString();
	
					String data = StringUtil.fill(cellStr, colLength[c]);
					sb.append(colSepData).append(data);
				}
				sb.append(colSepData).append(newLine);
			}
			// MULTIPLE line in one or more cells
			else
			{
				for (int l=0; l<maxCellLineCountOnThisRow; l++)
				{
					// |col1|col2   |col3|\n
					for (int c=0; c<cols; c++)
					{
						Object cellObj = tableData.get(r).get(c);
						String cellStr = cellObj == null ? "" : cellObj.toString();

						// first line
						if (l == 0)
						{
							// this cell has multiple lines, so just choose first line
							if ( cellObj instanceof String[] )
							{
								String[]sa = (String[]) cellObj;

								cellStr = sa[0];
							}
							String data = StringUtil.fill(cellStr, colLength[c]);
							sb.append(colSepData).append(data);
						}
						else // next of the lines
						{
							// this cell has multiple lines
							if ( cellObj instanceof String[] )
							{
								String[]sa = (String[]) cellObj;

								cellStr = "";
								if (l < sa.length)
									cellStr = sa[l];
							}
							else
							{
								cellStr = "";
							}
							String data = StringUtil.fill(cellStr, colLength[c]);
							sb.append(colSepData).append(data);
						}
					}
					sb.append(colSepData).append(newLine);
				}
			}
		}

		//-------------------------------------------
		// Print the TABLE FOOTER
		//-------------------------------------------
		// +------+------+-----+\n
		for (int c=0; c<cols; c++)
		{
			String line = StringUtil.replicate(lineSpace, colLength[c]);
			sb.append(colSepOther).append(line);
		}
		sb.append(colSepOther).append(newLine);
		sb.append("Rows ").append(copiedRows).append(newLine);
		
		return sb.toString();
	}

//	public static String exceptionToString(Throwable throwable)
//	{
//		return ExceptionUtils.getStackTrace(throwable);
//	}
	public static String exceptionToString(final Throwable throwable) 
	{
		if (throwable == null)
			return null;

		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw, true);

		throwable.printStackTrace(pw);
		
		return sw.getBuffer().toString();
	}
	
	public static String stackTraceToString(final StackTraceElement[] stea)
	{
		StringBuilder sb = new StringBuilder();

		String PREFIX   = "\tat ";
		String NEW_LINE = System.getProperty("line.separator");
		sb.append(NEW_LINE);

		//add each element of the stack trace
		for (StackTraceElement ste : stea)
		{
			sb.append(PREFIX);
			sb.append(ste);
			sb.append(NEW_LINE);
		}
		return sb.toString();
	}

	/** 
	 * Simply do toString() on non null object, and "" on null objects 
	 */
	public static String toStr(Object obj)
	{
		if (obj == null)
			return "";
		return obj.toString();
	}
	
	/** 
	 * Simply do toString() on non null object, and "" on null objects 
	 */
	public static String toStr(Object obj, Map<String, String> trMap)
	{
		String str = toStr(obj);
		if (str.equals(""))
			return str;

		// loop the trMap and replace strings
		if (trMap != null)
		{
    		for (Entry<String, String> entry : trMap.entrySet())
    			str = str.replace(entry.getKey(), entry.getValue());
		}

		return str;
	}

	/**
	 * conviniance wrapper for: String.format()
	 * 
	 * @link {@link String#format(String, Object...)}
	 * 
	 * @param format
	 * @param args
     * @return  A formatted string
	 */
	public static String format(String format, Object... args)
	{
		return String.format(format, args);
	}

	/**
	 * Add single quotes around the object if it looks like something that can be a String
	 * 
	 * @param obj
	 */
	public static String quotify(Object obj, String leftQuote, String rightQuote)
	{
		if (obj == null)
			return null;

		boolean addQuotes = false;
		if      (obj instanceof String) addQuotes = true;
		else if (obj instanceof Date  ) addQuotes = true;

		if ( ! addQuotes )
			return obj.toString();

		StringBuilder sb = new StringBuilder();
		sb.append(leftQuote).append(obj).append(rightQuote);
		return sb.toString();
	}
	public static String quotify(Object obj)
	{
		return quotify(obj, "'", "'");
	}
	public static String quotify(Object obj, String quoteStr)
	{
		return quotify(obj, quoteStr, quoteStr);
	}


	/**
	 * Remove all characters that are not within the span a..z using regexp '[^a-zA-Z0-9]'
	 * @param str
	 * @return
	 */
	public static String stripAllNonAlphaNum(String str)
	{
		if (str == null)
			return null;
		
		return str.replaceAll("[^a-zA-Z0-9]", "");
	}
	
	/**
	 * Replace any null string with a valid string
	 * @param input         Input string to check for null
	 * @param replacement   Replacement string if the input string is null
	 * @return
	 */
	public static String fixNull(String input, String replacement)
	{
		if (input == null)
			return replacement;

		return input;
	}

	/////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////
	//// TEST CODE
	/////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////

	public static void main(String[] args)
	{
		// Check if envVariableSubstitution() works with having a default value of another variable name, which does NOT seems to work...
		System.setProperty("DBXTUNE_ALARM_SOURCE_DIR", "-dbxtune-alarm-source-dir-");
		System.setProperty("DBXTUNE_HOME",             "-dbxtune-home-");
		System.out.println("envVariableSubstitution: ${DBXTUNE_ALARM_SOURCE_DIR:-${DBXTUNE_HOME}}/resources/alarm-handler-src = |" + envVariableSubstitution("${DBXTUNE_ALARM_SOURCE_DIR:-${DBXTUNE_HOME}}/resources/alarm-handler-src")+"|");

		System.setProperty("XXX", "-xxx-");
		System.setProperty("YYY", "-yyy-");
		System.out.println("envVariableSubstitution: ${XXX} = |" + envVariableSubstitution("${XXX}")+"|");
		System.out.println("envVariableSubstitution: ${YYY} = |" + envVariableSubstitution("${YYY}")+"|");
		System.out.println("envVariableSubstitution: ${ZZZ} = |" + envVariableSubstitution("${ZZZ}")+"|");
		System.out.println("envVariableSubstitution: ${ZZZ:-} = |" + envVariableSubstitution("${ZZZ:-}")+"|");
		System.out.println("envVariableSubstitution: ${XXX:-${YYY}} = |" + envVariableSubstitution("${XXX:-${YYY}}")+"|");
		System.out.println("envVariableSubstitution: ${AAA:-${BBB}} = |" + envVariableSubstitution("${AAA:-${BBB}}")+"|");
		System.exit(0);

		System.out.println("stripHtmlStartEnd(): |" + StringUtil.stripHtmlStartEnd("<html>12345</html>") + "|");
		System.out.println("stripHtmlStartEnd(): |" + StringUtil.stripHtmlStartEnd("<html> 1 2 3 4 5 </html>") + "|");
		System.exit(0);


		System.out.println("ltrim(): |" + StringUtil.ltrim(" 1 2 3 4 5 ") + "|");
		System.out.println("rtrim(): |" + StringUtil.rtrim(" 1 2 3 4 5 ") + "|");
		System.exit(0);

		System.out.println("splitOnCommasAllowQuotes(): " + StringUtil.toCommaStr(StringUtil.splitOnCommasAllowQuotes("string='',str='x,y',str3='''', int=99,int=null", true, false), "|") ); // size=4, toString=[1, 2, 'a,b,c', 'it''s true 2sq''''']

		System.out.println("splitOnCommasAllowQuotes(): " + StringUtil.toCommaStr(StringUtil.splitOnCommasAllowQuotes("1,2,'a,b,c', 'it''s true 2sq'''''", true, false), "|") ); // size=4, toString=[1, 2, 'a,b,c', 'it''s true 2sq''''']
		System.out.println("unquote(): " + StringUtil.unquote("'it''s 2 single-quotes'''''") ); // |it's 2 single-quotes''|
		System.exit(0);
		
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
