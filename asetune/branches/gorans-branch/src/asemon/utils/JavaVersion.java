package asemon.utils;

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
			int major = Integer.parseInt(ver[0]);
			int minor = Integer.parseInt(ver[1]);

			int maint  = 0;
//			int bugfix = 0;
			if (ver.length >= 3)
			{
				String[] maint_bugfix_arr = ver[2].split("_");
				maint  = Integer.parseInt(maint_bugfix_arr[0]);
				
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
			return (major*100) + (minor*10) + maint;
		}
		catch (Throwable e)
		{
			// TODO: handle exception
		}

		return VERSION_NOTFOUND;
	}
}
