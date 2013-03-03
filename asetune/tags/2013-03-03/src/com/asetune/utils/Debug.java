package com.asetune.utils;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Holds debug options that we later can check if a specific debug option is enabled or not
 * 
 * <pre>
 * if (Debug.hasDebug("doSomethingSpecial"))
 * {
 *     ... turn on whatever extra stuff that we need to do
 * }
 * </pre>
 * @author gorans
 */
public class Debug
{
	/** Known "predefined" debug options, and there description. */ 
	private static Map<String, String> _knownDebugOptions = new LinkedHashMap<String, String>();

	/** Simple set of strings, that will indicate if we have a debug option or not */ 
	private static Set<String> _debug = new HashSet<String>();

	private static boolean     _caseSensitive = true;

	/**
	 * Check if a specific debug option is set or not
	 * 
	 * @param debugOption name of the debug option to check for
	 * @return true if debug is set, false if not
	 */
	public static boolean hasDebug(String debugOption)
	{
		return _debug.contains( _caseSensitive ? debugOption : debugOption.toLowerCase() );
	}

	/**
	 * Add a debug option
	 * @param debugOption
	 */
	public static void addDebug(String debugOption)
	{
		if (debugOption == null) throw new IllegalArgumentException("debug option can't be null.");
		_debug.add( _caseSensitive ? debugOption : debugOption.toLowerCase() );
	}

	/**
	 * Remove a debug option
	 * @param debugOption
	 */
	public static void removeDebug(String debugOption)
	{
		if (debugOption == null) throw new IllegalArgumentException("debug option can't be null.");
		_debug.remove( _caseSensitive ? debugOption : debugOption.toLowerCase() );
	}

	/**
	 * Get the underlying structure that holds all available debug options.
	 * @return
	 */
	public static Set<String> getDebugs()
	{
		return _debug;
	}

	/**
	 * Get a String of all debug options that are enabled, use thei to debug what has been set or not
	 * @return
	 */
	public static String getDebugsString()
	{
		String ret = "";
		for (String str : _debug)
			ret += str + ", ";
		return ret;
	}
	

	/**
	 * Add predefined debug option and there descriptions
	 * 
	 * @param debugOption
	 * @param description
	 */
	public static void addKnownDebug(String debugOption, String description)
	{
		if (debugOption == null) throw new IllegalArgumentException("debug option can't be null.");
		if (description == null) throw new IllegalArgumentException("description can't be null.");

		_knownDebugOptions.put( debugOption, description );
	}

	/**
	 * Get the underlying structure that holds all "predefined" debug options.
	 * @return a Map<name, description> of all predefined debug options
	 */
	public static Map<String, String> getKnownDebugs()
	{
		return _knownDebugOptions;
	}
}
