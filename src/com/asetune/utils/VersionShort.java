package com.asetune.utils;

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

	
	public static void main(String[] args)
	{
		System.out.println("BEGIN: all test");

		if (toInt(10, 1, 1) != 100101) System.err.println("Test toInt-1: failed");
		if (toInt(1, 0, 0)  != 10000)  System.err.println("Test toInt-2: failed");
		
		if (toInt(10, 1, 1) != parse("10.1.1")) System.err.println("Test parse-1: failed");
		if (toInt(10, 1, 0) != parse("10.1"))   System.err.println("Test parse-2: failed");
		if (toInt(10, 1, 1) != parse("xxx 10.1.1 xxx")) System.err.println("Test parse-3: failed: ");
		
		System.out.println("END: all test");
	}
}
