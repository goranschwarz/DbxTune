package com.asetune.utils;

import org.apache.log4j.Logger;

public class Ver
{
	private static Logger _logger = Logger.getLogger(Ver.class);

//	/**
//	 * compose a long integer version number (9 digits, MMMMSSSPP) based on the more readable input
//	 *
//	 * @param ver 4 digit number (ex: 1254) if it's less than 4 digits it will be converted to that. if it's higher it will be truncated into 9999
//	 * @param sp  3 digit number (ex: 103)  if it's less than 3 digits it will be converted to that. if it's higher it will be truncated into 999
//	 * @param pl  2 digit number (ex: 1)    if it's less than 2 digits it will be converted to that. if it's higher it will be truncated into 99
//	 * 
//	 * @return a 9 digit number MMMMSSSPP  (MMMM=mainVersion, SSS=ServicePack/ESD#, PP=PatchLevel/ESD#SubLevel)
//	 */
//	public static int ver(int ver, int sp, int pl)
//	{
//		// convert small version numbers into larger ones: 1->1000, 12->1200, 157->1570
//		while (ver < 1000)
//			ver = ver * 10;
//
//		// don't exceed Max values for different parts
//		if (ver > 9999) ver = 9999;
//		if (sp  >  999) ver =  999;
//		if (pl  >   99) ver =   99;
//		
//		return (ver*100000) + (sp*100) + pl;
//	}

	/**
	 * compose a long integer version number (9 digits, MMMMSSSPP) based on the more readable input
	 *
	 * @param major 2 digit number
	 * @param minor 1 digit number
	 * @param maint 1 digit number
	 * @param sp    3 digit number (ex: 1, 50, 101)
	 * @param pl    2 digit number (ex: 1)
	 * @return
	 * 
	 * @return a 9 digit number MMMMSSSPP  (MMMM=mainVersion, SSS=ServicePack/ESD#, PP=PatchLevel/ESD#SubLevel)
	 */
	public static int ver(int major, int minor, int maint, int sp, int pl)
	{
		if (major < 10)
			_logger.warn("Version Converter. major="+major+", minor="+minor+",maint="+maint+",sp="+sp+", pl="+pl+". Major must be above 10 otherwise the calcuation will be wrong.");

		return    (major * 10000000)
				+ (minor * 1000000)
				+ (maint * 100000)
				+ (sp    * 100)
				+ pl
				;
	}

	public static int ver(int major, int minor, int maint, int sp)
	{
		return ver(major, minor, maint, sp, 0);
	}

	public static int ver(int major, int minor, int maint)
	{
		return ver(major, minor, maint, 0, 0);
	}

	public static int ver(int major, int minor)
	{
		return ver(major, minor, 0, 0, 0);
	}

}
