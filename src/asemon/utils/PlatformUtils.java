package asemon.utils;

import org.apache.log4j.Logger;

public class PlatformUtils 
{
	private static Logger _logger = Logger.getLogger(PlatformUtils.class);

//	public enum Platform {WIN, LINUX, MAC_OS, SOLARIS, OTHER} 
//	public enum Desktop  {WIN, KDE, GNOME, MAC_OS, OTHER} 
//	public enum Browser  {IE, FIREFOX} 

	public final static int Platform_WIN     = 0;
	public final static int Platform_LINUX   = 1;
	public final static int Platform_MAC_OS  = 2;
	public final static int Platform_SOLARIS = 3;
	public final static int Platform_OTHER   = 4;

	public final static int Desktop_WIN      = 10;
	public final static int Desktop_KDE      = 11;
	public final static int Desktop_GNOME    = 12;
	public final static int Desktop_MAC_OS   = 13;
	public final static int Desktop_OTHER    = 14;

	public final static int Browser_IE       = 20;
	public final static int Browser_FIREFOX  = 21;
	
	/*************************************************************************
	 * Gets the platform we are currently running on.
	 * @return a platform code.
	 ************************************************************************/
//	public static Platform getCurrentPlattform() 
	public static int getCurrentPlattform() 
	{
		String osName = System.getProperty("os.name");
		
		if (osName.startsWith("Windows")) 
		{
			_logger.trace("Detected Windows platform '"+osName+"'.");
			return Platform_WIN;
		} 
		if (osName.startsWith("Linux")) 
		{
			_logger.trace("Detected Linux platform '"+osName+"'.");
			return Platform_LINUX;
		} 
		if (osName.startsWith("MacOS")) 
		{
			_logger.trace("Detected Mac OS platform '"+osName+"'.");
			return Platform_MAC_OS;
		} 
		if (osName.startsWith("Solaris")) 
		{
			_logger.trace("Detected Solaris platform '"+osName+"'.");
			return Platform_SOLARIS;
		}
		
		return Platform_OTHER;
	}
	
	/*************************************************************************
	 * Gets the ID for the platform default browser.
	 * @return a browser ID, null if no supported browser was detected.
	 ************************************************************************/	
//	public static Browser getDefaultBrowser() 
	public static int getDefaultBrowser() 
	{
		// Use better logic to detect default browser?
		if (getCurrentPlattform() == Platform_WIN) 
		{
			_logger.trace("Detected Browser is InternetExplorer.");
			return Browser_IE;
		} 
		else 
		{
			_logger.trace("Detected Browser Firefox. (or possible just: Fallback?)");
			return Browser_FIREFOX;
		}
	}
	
	/*************************************************************************
	 * Gets the desktop that we are running on.
	 * @return the desktop identifier.
	 ************************************************************************/	
//	public static Desktop getCurrentDesktop() 
	public static int getCurrentDesktop() 
	{
		String osName = System.getProperty("os.name");
		
		if (osName.startsWith("Windows")) 
		{
			_logger.trace("Detected Windows desktop.");
			return Desktop_WIN;
		} 
		if (osName.startsWith("MacOS")) 
		{
			_logger.trace("Detected Mac OS desktop.");
			return Desktop_MAC_OS;
		} 

		if (   osName.startsWith("Linux") 
		    || osName.contains("Unix") 
		    || osName.startsWith("Solaris") 
		   )
		{
			if (isKDE()) 
			{
				_logger.trace("Detected KDE desktop.");
				return Desktop_KDE;
			}
			if (isGnome()) 
			{
				_logger.trace("Detected Gnome desktop.");
				return Desktop_GNOME;
			}
		} 
		_logger.trace("Detected Unknown desktop (fallback).");
		return Desktop_OTHER;
	}

	/*************************************************************************
	 * Checks if we are currently running under Gnome desktop.
	 * @return true if it is a Gnome else false.
	 ************************************************************************/
	private static boolean isGnome() 
	{
		return System.getenv("GNOME_DESKTOP_SESSION_ID") != null;
	}

	/*************************************************************************
	 * Checks if we are currently running under KDE desktop.
	 * @return true if it is a KDE else false. 
	 ************************************************************************/
	private static boolean isKDE() 
	{
		return System.getenv("KDE_SESSION_VERSION") != null;
	}
}
