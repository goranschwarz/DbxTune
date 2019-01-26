/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.asetune.utils;

import java.util.StringTokenizer;

/**
 * This class detects the Java-Version
 */
public class JavaVersion
{
	public static final int	VERSION_NOTFOUND	= -1;
//	public static final int	VERSION_1_0			= 100;
//	public static final int	VERSION_1_1			= 110;
//	public static final int	VERSION_1_2			= 120;
//	public static final int	VERSION_1_3			= 130;
//	public static final int	VERSION_1_4			= 140;
//	public static final int	VERSION_1_5			= 150;
//	public static final int	VERSION_1_6			= 160;
//	public static final int	VERSION_1_7			= 170;
//	public static final int	VERSION_1_8			= 180;
//	public static final int	VERSION_9			= 190;

	public static final int	VERSION_1_0			= 100;
	public static final int	VERSION_1_1			= 110;
	public static final int	VERSION_1_2			= 120;
	public static final int	VERSION_1_3			= 130;
	public static final int	VERSION_1_4			= 140;
	public static final int	VERSION_5			= 500;
	public static final int	VERSION_6			= 600;
	public static final int	VERSION_7			= 700;
	public static final int	VERSION_8			= 800;
	public static final int	VERSION_9			= 900;

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

		// IN Java 9 there is a new versioning string schema... http://openjdk.java.net/jeps/223
		String[] ver = verStr.split("\\.");

		if ( ver.length < 2 )
		{
			try
			{
    			// '9-ea' is for example Java9 EarlyAccess
    			if (verStr.indexOf('-') > 0)
    			{
    				String[] java9sa = verStr.split("-");
    				_major  = Integer.parseInt(java9sa[0]);
    				_minor  = 0;
    			}
    			else
    			{
    				_major  = Integer.parseInt(verStr);
    				_minor  = 0;
    			}
    			return (_major*100) + (_minor*10) + _maint;
			}
			catch(NumberFormatException ex)
			{
				// TODO: handle exception
			}
			
			return VERSION_NOTFOUND;
		}

		try
		{
			_major = Integer.parseInt(ver[0]);
			_minor = Integer.parseInt(ver[1]);

//			int bugfix/spLevel/pathLevel = 0;
			if (ver.length >= 3)
			{
				String[] maint_bugfix_arr = ver[2].split("_");
				_maint  = Integer.parseInt(maint_bugfix_arr[0]);
				
//				if (ver.length >= 2)
//					bugfix = Integer.parseInt(maint_bugfix_arr[1]);
			}

			// Java 9: looks like 9.x.y    
			// Since all releases before java 9 had 1.x.y we just need to look for 'larger than 1' to determen if its the new version schema
			// if it's the new version schema we just "do nothing"
			if (_major > 1)
			{
			}
			else if (_minor >= 5) // if java 5 and above (1.5.x) move the version parts one left. (1.5.1 -> 5.1.0) to follow the new version schema 
			{
				_major = _minor;
				_minor = _maint;
				_maint = 0; // or if we start to parse the "bugfix/spLevel/pathLevel" we can use that as _maint.
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
	 * If jave is above 9
	 * @return
	 */
	public static boolean isJava9orLater()
	{
		try
		{
			// Java 9 version-String Scheme: http://openjdk.java.net/jeps/223
			StringTokenizer st = new StringTokenizer(System.getProperty("java.version"), "._-+");
			int majorVersion = Integer.parseInt(st.nextToken());
			return majorVersion >= 9;
		}
		catch (Exception e)
		{
			return false;
		}
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
			System.out.println("               - 8 for Java 1.8");
			System.out.println("               - 9 for Java 1.9");
			System.out.println("");
			
			System.exit(1);
		}
		int checkForVersion = Integer.parseInt(args[0]);

		// JVM MAJOR version (which wont work for java 1.4 and below, but on those releases  getMajor() will always return 1, which isn't supported anymore...
		int javaVer = getMajor();

		if (javaVer >= checkForVersion)
		{
			// OK
			System.exit(0);
		}
		else
		{
			System.out.println("");
			System.out.println("===============================================================");
			System.out.println(" This application needs a runtime Java "+checkForVersion+" or higher.");
			System.out.println(" Current 'java.version' = " + System.getProperty("java.version"));
			System.out.println(" which is parsed into the Java (major) Version Number: " + JavaVersion.getMajor());
			System.out.println("---------------------------------------------------------------");
			System.out.println("");

			// TO LOW VERSION
			System.exit(1);
		}
	}
}
