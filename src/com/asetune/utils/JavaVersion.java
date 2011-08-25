package com.asetune.utils;

/**
 * This class detects the Java-Version
 */
public class JavaVersion
{
	public static final int	VERSION_NOTFOUND	= -1;
	public static final int	VERSION_1_0			= 100;
	public static final int	VERSION_1_1			= 110;
	public static final int	VERSION_1_2			= 120;
	public static final int	VERSION_1_3			= 130;
	public static final int	VERSION_1_4			= 140;
	public static final int	VERSION_1_5			= 150;
	public static final int	VERSION_1_6			= 160;
	public static final int	VERSION_1_7			= 170;

	private static int _major = -1;
	private static int _minor = -1;
	private static int _maint = 0;

	/**
	 * Returns the Version of Java.
	 */
	public static int getVersion()
	{
		String verStr = System.getProperty("java.version");
		if (verStr == null)
			return VERSION_NOTFOUND;

		String[] ver = verStr.split("\\.");
		if ( ver.length < 2 )
			return VERSION_NOTFOUND;

		try
		{
			_major = Integer.parseInt(ver[0]);
			_minor = Integer.parseInt(ver[1]);

//			int bugfix = 0;
			if (ver.length >= 3)
			{
				String[] maint_bugfix_arr = ver[2].split("_");
				_maint  = Integer.parseInt(maint_bugfix_arr[0]);
				
//				if (ver.length >= 2)
//					bugfix = Integer.parseInt(maint_bugfix_arr[1]);
			}

//			if ( major == 1 )
//			{
//				switch (minor)
//				{
//				case 0: return VERSION_1_0;
//				case 1: return VERSION_1_1;
//				case 2: return VERSION_1_2;
//				case 3: return VERSION_1_3;
//				case 4: return VERSION_1_4;
//				case 5: return VERSION_1_5;
//				case 6: return VERSION_1_6;
//				case 7: return VERSION_1_7;
//				}
//			}

			// ok nothing found so far, lets try to calculate a value
			return (_major*100) + (_minor*10) + _maint;
		}
		catch (Throwable e)
		{
			// TODO: handle exception
		}

		return VERSION_NOTFOUND;
	}


	/**
	 * Get MAJOR version of the java version <MAJOR>.<minor>.<maint>
	 * @return number if available, otherwise -1 or <code>JavaVersion.VERSION_NOTFOUND</code>
	 */
	public static int getMajor()
	{
		if (_major == -1 && _minor == -1)
			getVersion();
		return _major;
	}
	
	/**
	 * Get MINOR version of the java version <major>.<MINOR>.<maint>
	 * @return number if available, otherwise -1 or <code>JavaVersion.VERSION_NOTFOUND</code>
	 */
	public static int getMinor()
	{
		if (_major == -1 && _minor == -1)
			getVersion();
		return _minor;
	}
	
	/**
	 * Get MAINT version of the java version <major>.<minor>.<MAINT>
	 * @return number if available, otherwise -1 or <code>JavaVersion.VERSION_NOTFOUND</code>
	 */
	public static int getMaint()
	{
		if (_major == -1 && _minor == -1)
			getVersion();
		return _maint;
	}

	
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args)
	{
		if (args.length == 0)
		{
			System.out.println("");
			System.out.println("Usage: JavaVersion verInt");
			System.out.println("       verInt: should be the main java version you check for.");
			System.out.println("               - 4 for Java 1.4");
			System.out.println("               - 5 for Java 1.5");
			System.out.println("               - 6 for Java 1.6");
			System.out.println("               - 7 for Java 1.7");
			System.out.println("");
			
			System.exit(1);
		}
		int checkForVersion = Integer.parseInt(args[0]);

		// Note: JVM version 
		//       1.4 = Java 4
		//       1.5 = Java 5
		//       1.6 = Java 6
		int javaVer = getMinor();

		if (javaVer >= checkForVersion)
		{
			// OK
			System.exit(0);
		}
		else
		{
			System.out.println("");
			System.out.println("===============================================================");
			System.out.println(" This application needs a runtime JVM 1."+checkForVersion+" or higher.");
			System.out.println(" Current 'java.version' = " + System.getProperty("java.version"));
			System.out.println(" which is parsed into the Java Version Number: " + JavaVersion.getMinor());
			System.out.println("---------------------------------------------------------------");
			System.out.println("");

			// TO LOW VERSION
			System.exit(1);
		}
	}
}
