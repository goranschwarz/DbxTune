package com.asetune.utils;


/**
 * Helper class to parse and hold version information regarding Sybase products
 *   
 * @author gorans
 */
public class SybVersion
{
}
//public class SybVersion
//implements Comparable<SybVersion>
//{
////	private static Logger _logger = Logger.getLogger(SybVersion.class);
//
//	private int _major = 0;
//	private int _minor = 0;
//	private int _maint = 0;
//	private int _esd   = 0;
//	private int _ebf   = 0;
//	
//	public SybVersion(int major, int minor, int maintenance, int esd)
//	{
//		_major = major;
//		_minor = minor;
//		_maint = maintenance;
//		_esd   = esd;
//	}
//	public SybVersion(int majorMinorMaintEsd)
//	{
//	}
//
//	public int getMajor()       { return _major; }
//	public int getMinor()       { return _minor; }
//	public int getMaintenance() { return _maint; }
//	public int getEsd()         { return _esd;   }
//	public int getEbf()         { return _ebf;   }
//	
//	/** 
//	 * returns a 8 digit number in the form 11223344<p>
//	 * where 11 = Major, 22=Minor, 33=Maintenance, 44=ESD/rollup<p>
//	 * 
//	 * @return 12050412 if the Sybase Version vould be 12.5.4.esd#12
//	 */
//	public int toInt(int major, int minor, int maint, int esd)
//	{
//		if (major > 99) major = 99;
//		if (minor > 99) minor = 99;
//		if (maint > 99) maint = 99;
//		if (esd   > 99) esd   = 99;
//
//		return major * 1000000
//			 + minor * 10000 
//			 + maint * 100 
//			 + esd;
//	}
//
//	/** 
//	 * returns a 8 digit number in the form 11223344<p>
//	 * where 11 = Major, 22=Minor, 33=Maintenance, 44=ESD/rollup<p>
//	 * 
//	 * @return 12050412 if the Sybase Version vould be 12.5.4.esd#12
//	 */
//	public int toInt()
//	{
//		return toInt(_major, _minor, _maint, _esd);
//	}
//
//	/** 
//	 * returns a 4 (or 5) digit number in the form 1234<p>
//	 * where 1 = Major, 2=Minor, 3=Maintenance, 4=ESD/rollup<p>
//	 * Major can be between 1 and 99
//	 * 
//	 * @return 12549 if the Sybase Version vould be 12.5.4.esd#12
//	 */
//	public int toSmallInt()
//	{
//		int major = _major;
//		int minor = _minor;
//		int maint = _maint;
//		int esd   = _esd;
//
//		if (major > 99) major = 99;
//		if (minor >  9) minor = 9;
//		if (maint >  9) maint = 9;
//		if (esd   >  9) esd   = 9;
//
//		return major * 1000
//			 + minor * 100 
//			 + maint * 10 
//			 + esd;
//	}
//
//	@Override
//	public String toString()
//	{
//		StringBuilder sb = new StringBuilder();
//
//		sb.append(_major).append(".").append(_minor);
//		if (_maint > 0) sb.append(".").append(_maint);
//		if (_esd   > 0) sb.append(" ESD#").append(_esd);
//		if (_ebf   > 0) sb.append(" (EBF ").append(_ebf).append(")");
//
//		return sb.toString();
//	}
//
//	@Override
//	public boolean equals(Object obj)
//	{
//		if (obj instanceof SybVersion)
//			return compareTo( (SybVersion)obj ) == 0;
//		return super.equals(obj);
//	}
//
//	@Override
//	public int compareTo(SybVersion sybVersion)
//	{
//		int thisVerNum  = this.toInt();
//		int paramVerNum = sybVersion.toInt();
//
//		if (thisVerNum == paramVerNum)
//			return 0;
//		else if (thisVerNum < paramVerNum)
//			return -1;
//		else
//			return 1;
//	}
//
//
//	/** this SybVersion object is Greater Than or Equal the parameters */
//	public boolean gte(int major, int minor, int maint, int esd)
//	{
//		return toInt() >= toInt(major, minor, maint, esd);
//	}
//
//	/** this SybVersion object is Greater Than or Equal the parameters */
//	public boolean greaterThanOrEqual(int major, int minor, int maint, int esd)
//	{
//		return gte(major, minor, maint, esd);
//	}
//
//	/** this SybVersion object is Greater Than the parameters */
//	public boolean gt(int major, int minor, int maint, int esd)
//	{
//		return toInt() > toInt(major, minor, maint, esd);
//	}
//
//	/** this SybVersion object is Greater Than the parameters */
//	public boolean greaterThan(int major, int minor, int maint, int esd)
//	{
//		return gt(major, minor, maint, esd);
//	}
//
//	
//	/** this SybVersion object is Less Than or Equal the parameters */
//	public boolean lte(int major, int minor, int maint, int esd)
//	{
//		return toInt() <= toInt(major, minor, maint, esd);
//	}
//
//	/** this SybVersion object is Less Than or Equal the parameters */
//	public boolean lessThanOrEqual(int major, int minor, int maint, int esd)
//	{
//		return lte(major, minor, maint, esd);
//	}
//
//	/** this SybVersion object is Less Than the parameters */
//	public boolean lt(int major, int minor, int maint, int esd)
//	{
//		return toInt() < toInt(major, minor, maint, esd);
//	}
//
//	/** this SybVersion object is Less Than the parameters */
//	public boolean lessThan(int major, int minor, int maint, int esd)
//	{
//		return lt(major, minor, maint, esd);
//	}
//	
//	/**
//	 * Is this version between the two specified in the parameters
//	 * <p>
//	 * the type Between.GTE_LTE is used for the comparison: <code>this >= params && this <= params</code>
//	 * 
//	 * @param lowMajor
//	 * @param lowMinor
//	 * @param lowMaint
//	 * @param lowEsd
//	 * @param highMajor
//	 * @param highMinor
//	 * @param highMaint
//	 * @param highEsd
//	 * @return
//	 */
//	public boolean between(int lowMajor, int lowMinor, int lowMaint, int lowEsd, int highMajor, int highMinor, int highMaint, int highEsd)
//	{
//		return between(lowMajor, lowMinor, lowMaint, lowEsd, highMajor, highMinor, highMaint, highEsd, Between.GTE_LTE);
//	}
//
//	/**
//	 * Is this version between the two specified in the parameters
//	 * 
//	 * @param lowMajor
//	 * @param lowMinor
//	 * @param lowMaint
//	 * @param lowEsd
//	 * @param highMajor
//	 * @param highMinor
//	 * @param highMaint
//	 * @param highEsd
//	 * @param type
//	 * @return
//	 */
//	public boolean between(int lowMajor, int lowMinor, int lowMaint, int lowEsd, int highMajor, int highMinor, int highMaint, int highEsd, Between type)
//	{
//		if (type == Between.GTE_LTE)
//			return gte(lowMajor, lowMinor, lowMaint, lowEsd) && lte(highMajor, highMinor, highMaint, highEsd);
//
//		if (type == Between.GTE_LT)
//			return gte(lowMajor, lowMinor, lowMaint, lowEsd) && lt(highMajor, highMinor, highMaint, highEsd);
//
//		if (type == Between.GT_LTE)
//			return gt(lowMajor, lowMinor, lowMaint, lowEsd) && lte(highMajor, highMinor, highMaint, highEsd);
//
//		if (type == Between.GT_LT)
//			return gt(lowMajor, lowMinor, lowMaint, lowEsd) && lt(highMajor, highMinor, highMaint, highEsd);
//
//		// we should never end up here.
//		throw new RuntimeException("Unknown type of Between type "+type);
//	}
//
//	/** 
//	 * When calling between() method, this the boundaries for the between comparison 
//	 */
//	public enum Between
//	{
//		/** >= on low value, <= on high value */
//		GTE_LTE,
//		
//		/** >= on low value, < on high value */
//		GTE_LT,
//		
//		/** > on low value, <= on high value */
//		GT_LTE,
//		
//		/** > on low value, < on high value */
//		GT_LT
//	}
//
//	/**
//	 * Parses the Sybase version string into a number.<br>
//	 * The version string will be splitted on the character '/' into different
//	 * version parts. The second part will be used as the version string.<br>
//	 * 
//	 * The version part will then be splitted into different parts by the
//	 * delimiter '.'<br>
//	 * Four different version parts will be handled:
//	 * Major.Minor.Maintenance.Rollup<br>
//	 * Major version part can contain several characters, while the other
//	 * version parts can only contain 1 character (only the first character i
//	 * used).
//	 * 
//	 * @param versionStr
//	 *            the Sybase version string fetched from the database with select @@version
//	 * @return SybVersion object.
//	 * @throws ParseException
//	 */
//// FIXME: this is a ASE-CE version string
////	 Adaptive Server Enterprise/15.0.3/EBF 16748 Cluster Edition/P/x86_64/Enterprise Linux/asepyxis/2837/64-bit/FBO/Mon Jun  1 08:38:39 2009
//	public static SybVersion parse(String sybVersionString)
//	throws ParseException
//	{
//		SybVersion sybVersion = new SybVersion(0, 0, 0, 0);
//
//		String[] sybVersionParts = sybVersionString.split("/");
//		if (sybVersionParts.length > 0)
//		{
//			String sybVersionNumberStr = null;
//			String sybEsdStr = null;
//			String sybEbfStr = null;
//			// Scan the string to see if there are any part that looks like a version str (##.#)
//			for (int i=0; i<sybVersionParts.length; i++)
//			{
////				if ( sybVersionParts[i].matches("^[0-9][0-9][.][0-9][.][0-9]") && sybVersionNumberStr == null )
////				if ( sybVersionParts[i].matches("^[0-9][0-9][.][0-9]([.][0-9])*") && sybVersionNumberStr == null )
//				if ( sybVersionParts[i].matches("^[0-9][0-9][.][0-9].*") && sybVersionNumberStr == null )
//				{
//					sybVersionNumberStr = sybVersionParts[i];
//				}
//
//				if ( sybVersionParts[i].indexOf("ESD#") >= 0 && sybEsdStr == null)
//				{
//					sybEsdStr = sybVersionParts[i];
//				}
//
//				if ( sybVersionParts[i].indexOf("EBF ") >= 0 && sybEbfStr == null)
//				{
//					sybEbfStr = sybVersionParts[i];
//				}
//			}
//
//			if (sybVersionNumberStr == null)
//			{
//				String msg = "There Sybase version string seems to be faulty, cant find any '##.#' in the version number string '" + sybVersionString + "'.";
//				throw new ParseException(msg, 0);
//			}
//
//			String[] sybVersionNumberParts = sybVersionNumberStr.split("\\.");
//			if (sybVersionNumberParts.length > 1)
//			{
//				// Version parts can contain characters...
//				// hmm version could be: 12.5.3a
//				try
//				{
//					String versionPart = null;
//					// MAJOR version: ( <12>.5.2.1 - MAJOR.minor.maint.rollup )
//					if (sybVersionNumberParts.length >= 1)
//					{
//						versionPart = sybVersionNumberParts[0].trim();
//						sybVersion._major = Integer.parseInt(versionPart);
//					}
//
//					// MINOR version: ( 12.<5>.2.1 - major.MINOR.maint.rollup )
//					if (sybVersionNumberParts.length >= 2)
//					{
//						versionPart = sybVersionNumberParts[1].trim();
//						sybVersion._minor = Integer.parseInt(versionPart);
//					}
//
//					// MAINTENANCE version: ( 12.5.<2>.1 - major.minor.MAINT.rollup )
//					if (sybVersionNumberParts.length >= 3)
//					{
//						versionPart = sybVersionNumberParts[2].trim();
//						sybVersion._maint = Integer.parseInt(versionPart);
//					}
//
//					// ROLLUP version: ( 12.5.2.<1> - major.minor.maint.ROLLUP )
//					if (sybVersionNumberParts.length >= 4)
//					{
//						versionPart = sybVersionNumberParts[3].trim();
//						sybVersion._esd = Integer.parseInt(versionPart);
//					}
//					else // go and check for ESD string, which is another way of specifying ROLLUP
//					{
//						if (sybEsdStr != null)
//						{
//							int start = sybEsdStr.indexOf("ESD#");
//							if (start >= 0)
//								start += "ESD#".length();
//
//							// set end to first NON digit (or end of string)
//							int end = start;
//							for (; end<sybEsdStr.length(); end++)
//							{
//								if ( ! Character.isDigit(sybEsdStr.charAt(end)) )
//									break;
//							}
//
//							if (start != -1)
//							{
//								try
//								{
//									versionPart = sybEsdStr.trim().substring(start, end);
//									sybVersion._esd = Integer.parseInt(versionPart);
//								}
//								catch (RuntimeException e) // NumberFormatException,
//								{
//									//String msg = "Problems converting some part(s) of the ESD# in the version string '" + sybVersionNumberStr + "' into a number. ESD# string was '"+versionPart+"'. The version number will be set to " + sybVersion;
//									String msg = "Problems converting some part(s) of the ESD# in the version string '" + sybVersionNumberStr + "' into a number. ESD# string was '"+versionPart+"'.";
//									throw new ParseException(msg, 0);
//								}
//							}
//						}
//					}
//
//					// Get any EBF number
//					if (sybEbfStr != null)
//					{
//						int start = sybEbfStr.indexOf("EBF ");
//						if (start >= 0)
//							start += "EBF ".length();
//						int end = sybEbfStr.indexOf(" ", start);
//						if (end == -1)
//							end = sybEbfStr.length();
//
//						if (start != -1)
//						{
//							try
//							{
//								versionPart = sybEbfStr.trim().substring(start, end);
//								sybVersion._ebf = Integer.parseInt(versionPart);
//							}
//							catch (RuntimeException e) // NumberFormatException,
//							{
//								//_logger.warn("Problems converting some part(s) of the EBF in the version string '" + sybVersionNumberStr + "' into a number. EBF string was '"+versionPart+"'. The version number will be set to " + sybVersion);
//								String msg = "Problems converting some part(s) of the EBF in the version string '" + sybVersionNumberStr + "' into a number. EBF string was '"+versionPart+"'..";
//								throw new ParseException(msg, 0);
//							}
//						}
//					}
//					
//				}
//				// catch (NumberFormatException e)
//				catch (RuntimeException e) // NumberFormatException,
//											// IndexOutOfBoundsException
//				{
//					//_logger.warn("Problems converting some part(s) of the version string '" + sybVersionNumberStr + "' into a number. The version number will be set to " + sybVersion);
//					String msg = "Problems converting some part(s) of the version string '" + sybVersionNumberStr + "' into a number. The version number could be set to " + sybVersion;
//					throw new ParseException(msg, 0);
//				}
//			}
//			else
//			{
//				//_logger.warn("There Sybase version string seems to be faulty, cant find any '.' in the version number subsection '" + sybVersionNumberStr + "'.");
//				String msg = "There Sybase version string seems to be faulty, cant find any '.' in the version number subsection '" + sybVersionNumberStr + "'.";
//				throw new ParseException(msg, 0);
//			}
//		}
//		else
//		{
//			//_logger.warn("There Sybase version string seems to be faulty, cant find any / in the string '" + sybVersionString + "'.");
//			String msg = "There Sybase version string seems to be faulty, cant find any / in the string '" + sybVersionString + "'.";
//			throw new ParseException(msg, 0);
//		}
//
//		return sybVersion;
//	}
//
//	
//	
//	
////	1> select @@version
////	2> go
////
////	 -----------------------------------------------------------------------------------------------------------------------------------------
////	 Adaptive Server Enterprise/15.5/EBF 18157 SMP ESD#2/P/NT (IX86)/Windows 2003/asear155/2514/32-bit/OPT/Wed Aug 25 05:31:40 2010
////
////	(1 row affected)
////	1> sp_version
////	2> go
////	 Script            Version                                                                                             Status
////	 ----------------- --------------------------------------------------------------------------------------------------- --------
////	 ODBC MDA Scripts  15.5.0.1012.1000/Tue 04-06-2010 20:23:52.35                                                         Complete
////	 OLEDB MDA Scripts 15.5.0.1012.1000/Tue 04-06-2010 20:23:52.35                                                         Complete
////	 installcommit     15.0.3/EBF 16550 ESD#1/P/NT (IX86)/Windows 2003/ase1503/2680/32-bit/OPT/Thu Mar 05 01:03:17 2009    Complete
////	 installjdbc       jConnect (TM) for JDBC(TM)/7.00(Build 26502)/P/EBF17821/JDK16/Tue Apr  6 11:59:33 2010              Complete
////	 installjsdb       15.0.3/EBF 16550 ESD#1/P/NT (IX86)/Windows 2003/ase1503/2680/32-bit/OPT/Thu Mar 05 01:03:17 2009    Complete
////	 installmaster     15.5/EBF 18157 SMP ESD#2/P/NT (IX86)/Windows 2003/asear155/2514/32-bit/OPT/Wed Aug 25 05:45:22 2010 Complete
////	 installmodel      15.0.3/EBF 16550 ESD#1/P/NT (IX86)/Windows 2003/ase1503/2680/32-bit/OPT/Thu Mar 05 01:03:17 2009    Complete
////	 installpcidb      15.0.3/EBF 16550 ESD#1/P/NT (IX86)/Windows 2003/ase1503/2680/32-bit/OPT/Thu Mar 05 01:03:17 2009    Complete
////	 installsecurity   15.5/EBF 17218 SMP/P/NT (IX86)/Windows 2003/ase155/2391/32-bit/OPT/Mon Nov 09 14:32:38 2009         Complete
////	 montables         15.5/18157/P/NT (IX86)/Windows 2003/asear155/2514/32-bit/OPT/Wed Aug 25 05:22:07 2010               Complete
//
//	private static void test(String versionStr)
//	{
//		System.out.println("==================================================================");
//		System.out.println("Input str           '"+versionStr+"'.");
//
//		SybVersion sybVer = null;
//		try
//		{
//			sybVer = SybVersion.parse(versionStr);
//			System.out.println("SybVer.toString()   '"+sybVer.toString()+"'.");
//			System.out.println("SybVer.toInt()      '"+sybVer.toInt()+"'.");
//			System.out.println("SybVer.toSmallInt() '"+sybVer.toSmallInt()+"'.");
//			System.out.println("compareTo(15.5.0.0) '"+sybVer.compareTo(new SybVersion(15, 5, 0, 0))+"'.");
//			System.out.println("is above 15.5.0.0   '"+(sybVer.compareTo(new SybVersion(15, 5, 0, 0)) > 0)+"'.");
//			System.out.println("is    >= 15.5.0.0   '"+(sybVer.compareTo(new SybVersion(15, 5, 0, 0)) >= 0)+"'.");
//		}
//		catch (ParseException e)
//		{
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		System.out.println("------------------------------------------------------------------");
//		System.out.println("");
//	}
//	public static void main(String[] args)
//	{
//		Properties log4jProps = new Properties();
//		//log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
//		log4jProps.setProperty("log4j.rootLogger", "TRACE, A1");
//		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
//		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
//		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
//		PropertyConfigurator.configure(log4jProps);
//
//		test("Adaptive Server Enterprise/15.0.3/EBF 16748 Cluster Edition/P/x86_64/Enterprise Linux/asepyxis/2837/64-bit/FBO/Mon Jun  1 08:38:39 2009");
//		test("Adaptive Server Enterprise/15.5/EBF 18157 SMP ESD#2/P/NT (IX86)/Windows 2003/asear155/2514/32-bit/OPT/Wed Aug 25 05:31:40 2010");
//		test("15.5/18157/P/NT (IX86)/Windows 2003/asear155/2514/32-bit/OPT/Wed Aug 25 05:22:07 2010");
//		test("jConnect (TM) for JDBC(TM)/7.00(Build 26502)/P/EBF17821/JDK16/Tue Apr  6 11:59:33 2010");
//		test("15.5.0.1012.1000/Tue 04-06-2010 20:23:52.35");
//		test("Replication Server/15.2/P/NT (IX86)/Windows 2003/1/OPT/Thu Feb 05 18:34:37 2009");
//		test("Adaptive Server Enterprise/12.5.4/EBF 15400 ESD#7.1/P/Sun_svr4/OS 5.8/ase1254/2097/64-bit/FBO/Thu Jan 17 07:42:59 2008");
//		test("Adaptive Server Enterprise/15.0.3/EBF 17686 ESD#1.1 RELSE/P/Sun_svr4/OS 5.8/ase1503/2681/64-bit/FBO/Thu Aug 20 14:20:57 2009");
//		test("Adaptive Server Enterprise/15.0.3/EBF 17686 ESD#10 RELSE/P/Sun_svr4/OS 5.8/ase1503/2681/64-bit/FBO/Thu Aug 20 14:20:57 2009");
//		test("Adaptive Server Enterprise/15.0.3/EBF 17686 ESD#99.5 RELSE/P/Sun_svr4/OS 5.8/ase1503/2681/64-bit/FBO/Thu Aug 20 14:20:57 2009");
//	}
//}
