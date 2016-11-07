/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.config.dict;



public class MonTablesDictionaryManager
{
    /** Log4j logging. */
//	private static Logger _logger          = Logger.getLogger(MonTablesDictionary.class);

	/** Instance variable */
	private static MonTablesDictionary _instance = null;


	public static boolean hasInstance()
	{
		return (_instance != null);
	}

	public static void setInstance(MonTablesDictionary instance)
	{
		_instance = instance;
	}

	public static MonTablesDictionary getInstance()
	{
//		if (_instance == null)
//			_instance = new MonTablesDictionaryDefault();

		if (_instance == null)
			throw new RuntimeException("No MonTablesDictionary has been set...");

		return _instance;
	}

	/**
	 * Reset the dictionary, this so we can get new ones later<br>
	 * Most possible called from disconnect() or similar
	 */
	public static void reset()
	{
		if (_instance != null)
			_instance.reset();
	}
}
