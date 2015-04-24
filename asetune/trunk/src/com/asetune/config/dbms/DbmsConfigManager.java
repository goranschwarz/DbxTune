package com.asetune.config.dbms;

import java.util.LinkedHashMap;
import java.util.Map;


public class DbmsConfigManager
{
	/** Instance variable */
	private static IDbmsConfig _instance = null;

	private static Map<String, IDbmsConfigText> _textInstances = new LinkedHashMap<String, IDbmsConfigText>();
	
	/** check if we got an instance or not */
	public static boolean hasInstance()
	{
		return (_instance != null);
	}

	/** Get a instance of the class */
	public static IDbmsConfig getInstance()
	{
		return _instance;
	}

	/** Get a instance of the class */
	public static void setInstance(IDbmsConfig dbmsConfig)
	{
		_instance = dbmsConfig;
	}
}
