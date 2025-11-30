/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionShort
{

	/**
	 * Parse a string to check if it contains a version string in the form: major.minor[.maintenance]
	 * @param version string
	 * 
	 * @return version number as an integer version... holding (major, minor, maintenance) 2 "positions" each in the integer<br>
	 *         version "10.2.0" is returned as 100200 <br>
	 *         version "3.3.9"  is returned as  30309 <br>
	 *         If a proper version string can't be found, -1 is returned.
	 */
	public static int parse(String version)
	{
		int intVersion = -1;
		
		if (version == null)
			return intVersion;
		
		String regexp = "(\\d+)\\.(\\d+)(?:\\.(\\d+))?"; // search for versions like 1.2[.3]
        Pattern versionPattern = Pattern.compile(regexp);
        Matcher m = versionPattern.matcher(version);
		if (m.find())
		{
			String g1 = m.group(1);
			String g2 = m.group(2);
			String g3 = m.group(3);
			
			if (g1 == null) g1 = "0";
			if (g2 == null) g2 = "0";
			if (g3 == null) g3 = "0";
			
			int major = Integer.parseInt(g1);
			int minor = Integer.parseInt(g2);
			int maint = Integer.parseInt(g3);

			intVersion = toInt(major, minor, maint);
		}
		return intVersion;
	}

	/**
	 * Constructs an integer with 2 "chars" per major, minor, maintenance 
	 *
	 * @param major
	 * @param minor
	 * @param maint
	 * 
	 * @return version number as an integer version... holding (major, minor, maintenance) 2 "positions" each in the integer<br>
	 *         version "10.2.0" is returned as 100200 <br>
	 *         version "3.3.9"  is returned as  30309 <br>
	 */
	public static int toInt(int major, int minor, int maint)
	{
		if (major > 99) throw new IllegalArgumentException("VersionShort.toint(major="+major+", minor="+minor+", maint="+maint+"): major can't be above 99.");
		if (minor > 99) throw new IllegalArgumentException("VersionShort.toint(major="+major+", minor="+minor+", maint="+maint+"): minor can't be above 99.");
		if (maint > 99) throw new IllegalArgumentException("VersionShort.toint(major="+major+", minor="+minor+", maint="+maint+"): maint can't be above 99.");

		if (major < 0)  throw new IllegalArgumentException("VersionShort.toint(major="+major+", minor="+minor+", maint="+maint+"): major can't be below 0.");
		if (minor < 0)  throw new IllegalArgumentException("VersionShort.toint(major="+major+", minor="+minor+", maint="+maint+"): minor can't be below 0.");
		if (maint < 0)  throw new IllegalArgumentException("VersionShort.toint(major="+major+", minor="+minor+", maint="+maint+"): maint can't be below 0.");

		return    ( major * 10000 )
				+ ( minor * 100 )
				+ ( maint * 1 )
				;
	}

	
	public static String toStr(int version)
	{
		// 5 digit version number
		if (version > 10000) // 1.00.00
		{
			// The below is probably wrong
			int major       = version                                     / 10000;
			int minor       =(version -  (major * 10000))                 / 100;
			int maintenance =(version - ((major * 10000) + (minor * 100))); // Take whats left, when subtracting major and minor

//			System.out.println("VersionShort.toStr("+version+") <-- '"+major + "." + minor + "." + maintenance+"'");
			return major + "." + minor + "." + maintenance;
		}

		return "unknown int("+version+")";
	}

	
	public static void main(String[] args)
	{
		System.out.println("BEGIN: all test");

		if (toInt(10, 1, 1) != 100101) System.err.println("Test toInt-1: failed");
		if (toInt(1, 0, 0)  != 10000)  System.err.println("Test toInt-2: failed");
		
		if (toInt(10, 1, 1) != parse("10.1.1")) System.err.println("Test parse-1: failed");
		if (toInt(10, 1, 0) != parse("10.1"))   System.err.println("Test parse-2: failed");
		if (toInt(10, 1, 1) != parse("xxx 10.1.1 xxx")) System.err.println("Test parse-3: failed: ");
		
		if ( ! toStr(12345).equals("1.23.45") ) System.err.println("Test toStr(12345): failed: got result '"+toStr(12345)+"', expected '1.23.45'.");
		if ( ! toStr(30310).equals("3.3.10") ) System.err.println("Test toStr(12345): failed: got result '"+toStr(30310)+"', expected '3.3.10'.");
		
		
		System.out.println("END: all test");
	}
}
