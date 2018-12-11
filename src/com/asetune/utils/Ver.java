package com.asetune.utils;

import org.apache.log4j.Logger;

import com.asetune.sql.DbmsVersionHelperOracle;
import com.asetune.sql.DbmsVersionHelperSimple;
import com.asetune.sql.DbmsVersionHelperSqlServer;
import com.asetune.sql.DbmsVersionHelperSybase;
import com.asetune.sql.IDbmsVersionHelper;

public class Ver
{
	private static Logger _logger = Logger.getLogger(Ver.class);
	public static boolean majorVersion_mustBeTenOrAbove = true;
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

//	/**
//	 * compose a long integer version number (9 digits, MMMMSSSPP) based on the more readable input
//	 *
//	 * @param major 2 digit number
//	 * @param minor 1 digit number
//	 * @param maint 1 digit number
//	 * @param sp    3 digit number (ex: 1, 50, 101)
//	 * @param pl    2 digit number (ex: 1)
//	 * @return
//	 * 
//	 * @return a 9 digit number MMMMSSSPP  (MMMM=mainVersion, SSS=ServicePack/ESD#, PP=PatchLevel/ESD#SubLevel)
//	 */
//	public static int ver(int major, int minor, int maint, int sp, int pl)
//	{
//		if (major < 10 && majorVersion_mustBeTenOrAbove)
//			_logger.warn("Version Converter. major="+major+", minor="+minor+",maint="+maint+",sp="+sp+", pl="+pl+". Major must be above 10 otherwise the calcuation will be wrong.");
//
//		return    (major * 100_000_00)
//				+ (minor *  10_000_00)
//				+ (maint *   1_000_00)
//				+ (sp    *       1_00)
//				+ pl
//				;
//	}
//
//	public static int ver(int major, int minor, int maint, int sp)
//	{
//		return ver(major, minor, maint, sp, 0);
//	}
//
//	public static int ver(int major, int minor, int maint)
//	{
//		return ver(major, minor, maint, 0, 0);
//	}
//
//	public static int ver(int major, int minor)
//	{
//		return ver(major, minor, 0, 0, 0);
//	}
	/**
	 * compose a long integer version number (9 digits, MMMMSSSPP) based on the more readable input
	 *
	 * @param major 1-2 digit number
	 * @param minor 1-2 digit number
	 * @param maint 1-2 digit number
	 * @param sp    1-4 digit number (ex: 1, 50, 101)
	 * @param pl    1-4 digit number (ex: 1)
	 * @return
	 * 
	 * @return a 13+ digit number MmMmMmSsssPppp  (Mm=mainVersion, Mm=minorVersion, Mm=maintenanceVersion, Ssss=ServicePack/ESD#, Pppp=PatchLevel/ESD#SubLevel)
	 */
	public static long ver(int major, int minor, int maint, int sp, int pl)
	{
//		if (major < 10 && majorVersion_mustBeTenOrAbove)
//			_logger.warn("Version Converter. major="+major+", minor="+minor+",maint="+maint+",sp="+sp+", pl="+pl+". Major must be above 10 otherwise the calcuation will be wrong.");

//		return    (major * 1_00_00_0000_0000L)
//				+ (minor *    1_00_0000_0000L)
//				+ (maint *       1_0000_0000L)
//				+ (sp    *            1_0000L)
//				+ pl
//				;
		long v1 = (long) major * 1_00_00_0000_0000L;
		long v2 = (long) minor *    1_00_0000_0000L;
		long v3 = (long) maint *       1_0000_0000L;
		long v4 = (long) sp    *            1_0000L;
		long v5 = (long) pl;

//		System.out.println("v1["+StringUtil.right(""+major,4)+"] - "+v1);
//		System.out.println("v2["+StringUtil.right(""+minor,4)+"] - "+v2);
//		System.out.println("v3["+StringUtil.right(""+maint,4)+"] - "+v3);
//		System.out.println("v4["+StringUtil.right(""+sp   ,4)+"] - "+v4);
//		System.out.println("v5["+StringUtil.right(""+pl   ,4)+"] - "+v5);
//		System.out.println("<<<- "+(v1 + v2 + v3 + v4 + v5));
		
		return v1 + v2 + v3 + v4 + v5;
	}

	public static long ver(int major, int minor, int maint, int sp)
	{
		return ver(major, minor, maint, sp, 0);
	}

	public static long ver(int major, int minor, int maint)
	{
		return ver(major, minor, maint, 0, 0);
	}

	public static long ver(int major, int minor)
	{
		return ver(major, minor, 0, 0, 0);
	}

	
	

	private static IDbmsVersionHelper _dbmsVersionHelper = null;
	public static IDbmsVersionHelper getDbmsVersionHelper() { return _dbmsVersionHelper; }
	public static void setDbmsVersionHelper(IDbmsVersionHelper helper) { _dbmsVersionHelper = helper; }

	public static String versionNumToStr(long version)
	{
		int major = versionNumPart(version, VERSION_MAJOR);
		int minor = versionNumPart(version, VERSION_MINOR);
		int maint = versionNumPart(version, VERSION_MAINTENANCE);
		int sp    = versionNumPart(version, VERSION_SERVICE_PACK);
		int pl    = versionNumPart(version, VERSION_PATCH_LEVEL);

		if (_dbmsVersionHelper != null)
		{
			return _dbmsVersionHelper.versionNumToStr(version, major, minor, maint, sp, pl);
		}
		else
		{
			return defaultVersionNumToStr(version, major, minor, maint, sp, pl);
		}
	}

	public static String versionNumToStr(long version, String dbmsProductName)
	{
		int major = versionNumPart(version, VERSION_MAJOR);
		int minor = versionNumPart(version, VERSION_MINOR);
		int maint = versionNumPart(version, VERSION_MAINTENANCE);
		int sp    = versionNumPart(version, VERSION_SERVICE_PACK);
		int pl    = versionNumPart(version, VERSION_PATCH_LEVEL);

		IDbmsVersionHelper dbmsVersionHelper = null;
		if (DbUtils.isProductName(dbmsProductName, DbUtils.DB_PROD_NAME_SYBASE_ASE, DbUtils.DB_PROD_NAME_SYBASE_ASA, DbUtils.DB_PROD_NAME_SYBASE_IQ, DbUtils.DB_PROD_NAME_SYBASE_RAX, DbUtils.DB_PROD_NAME_SYBASE_RS, DbUtils.DB_PROD_NAME_SYBASE_RSDA, DbUtils.DB_PROD_NAME_SYBASE_RSDRA))
			dbmsVersionHelper = new DbmsVersionHelperSybase();
		else if (DbUtils.isProductName(dbmsProductName, DbUtils.DB_PROD_NAME_MSSQL))
			dbmsVersionHelper = new DbmsVersionHelperSqlServer();
		else if (DbUtils.isProductName(dbmsProductName, DbUtils.DB_PROD_NAME_ORACLE))
			dbmsVersionHelper = new DbmsVersionHelperOracle();
		else
			dbmsVersionHelper = new DbmsVersionHelperSimple();
		
		return dbmsVersionHelper.versionNumToStr(version, major, minor, maint, sp, pl);
	}

	
	public static String defaultVersionNumToStr(long version, int major, int minor, int maintenance, int servicePack, int patchLevel)
	{
//System.out.println("defaultVersionNumToStr(version='"+version+"', major='"+major+"', minor'"+minor+"', maintenance'"+maintenance+"', servicePack'"+servicePack+"', patchLevel'"+patchLevel+"'. )");

		String verStr = "";
		if (maintenance == 0)
			verStr = major + "." + minor;
		else
			verStr = major + "." + minor + "." + maintenance;

		if (major >= 16)
		{
			if (servicePack > 0)
				verStr += " SP" + String.format("%02d", servicePack);
			if (patchLevel > 0)
				verStr += " PL" + String.format("%02d", patchLevel);

			// <<<<<<---------- RETURN HERE ------------------
			return verStr;
		}
		if (major >= 15 && minor >= 7 && servicePack >= 50)
		{
			verStr += " SP" + servicePack;

			// <<<<<<---------- RETURN HERE ------------------
			return verStr;
		}
		
		// all other below 16.0 and 15.7 SP50
		if (servicePack > 0)
		{
			String esdStr = Integer.toString(servicePack);
			if (patchLevel > 0)
				esdStr += "." + patchLevel;
			
			// <<<<<<---------- RETURN HERE ------------------
			return verStr + " ESD#" + esdStr;
		}

		return verStr;
	}
	
	/** 
	 * Convert a int version to a string version 
	 * <p>
	 * <code>15030 will be "15.0.3"</code>
	 * <code>15031 will be "15.0.3 ESD#1"</code>
	 */
	public static String FIXME_versionIntToStr(int version)
	{
		// 9 digit version num   // large version number (12.5.4 ESD#10.1)  125401001   (MMMMSSSPP: MMMM(1254)=MajorMinorMaintenance, SSS(010)=ServicePack/EsdLevel, PP(01)=PatchLevel/SubEsdLevel)
		if (version > 100000000) // large version number (15.7   SP100)     157010001   (MMMMSSSPP: MMMM(1570)=MajorMinorMaintenance, SSS(100)=ServicePack/EsdLevel, PP(01)=PatchLevel/SubEsdLevel)
		{                        // large version number (16.0   SP01 PL01) 160000101   (MMMMSSSPP: MMMM(1600)=MajorMinorMaintenance, SSS(001)=ServicePack/EsdLevel, PP(01)=PatchLevel/SubEsdLevel)
			int baseVer     = version / 100000;
			int major       = baseVer / 100;
			int minor       = baseVer % 100 / 10;
			int maintenance = baseVer % 10;
			int servicePack = version % 100000 / 100;
			int patchLevel  = version % 100;

//            System.out.println("");
//            System.out.println("versionNumToStr(): version     = " + version);
//            System.out.println("                   baseVer     = " + baseVer);
//            System.out.println("                   major       = " + major);
//            System.out.println("                   minor       = " + minor);
//            System.out.println("                   maintenance = " + maintenance);
//            System.out.println("                   servicePack = " + servicePack);
//            System.out.println("                   patchLevel  = " + patchLevel);

			// Set base version M.M[.M]
			String verStr = "";
			if (maintenance == 0)
				verStr = major + "." + minor;
			else
				verStr = major + "." + minor + "." + maintenance;

			// Version 16
			if (major >= 16)
			{
				if (servicePack > 0)
					verStr += " SP" + String.format("%02d", servicePack);
				if (patchLevel > 0)
					verStr += " PL" + String.format("%02d", patchLevel);

				// <<<<<<---------- RETURN HERE ------------------
				return verStr;
			}

			// Version 15.7 SP50 and over
			if (major >= 15 && minor >= 7 && servicePack >= 50)
			{
				verStr += " SP" + servicePack;

				// <<<<<<---------- RETURN HERE ------------------
				return verStr;
			}

			// all other below 16.0 and 15.7 SP50
			if (servicePack > 0)
			{
    			String esdStr = Integer.toString(servicePack);
    			if (patchLevel > 0)
    				esdStr += "." + patchLevel;
    			
    			// <<<<<<---------- RETURN HERE ------------------
    			return verStr + " ESD#" + esdStr;
			}

			return verStr;
		}
		// 7 digit version number
		else if (version > 1000000) // medium version number 1570100   (major minor maintenance EsdLevel/ServicePack)
		{
			int major       = version                                         / 100000;
			int minor       =(version -  (major * 100000))                    / 10000;
			int maintenance =(version - ((major * 100000) + (minor * 10000))) / 1000;
			int servicePack = version - ((major * 100000) + (minor * 10000) + (maintenance * 1000));
	
			if (servicePack == 0)
			{
				if (maintenance == 0)
					return major + "." + minor;
				else
					return major + "." + minor + "." + maintenance;
			}
			else if (servicePack < 100)
			{
				// ASE 15.7 (and above): if SP 50 and above write SP instead of ESD#x.y 
				if (major >= 15 && minor >= 7 && servicePack >= 50)
				{
					return major + "." + minor + "." + maintenance + " SP" + servicePack;
				}
				else
				{
					int mainEsd = servicePack / 10;
					int subEsd  = servicePack % 10;
					String esdStr = Integer.toString(mainEsd);
					if (subEsd > 0)
						esdStr = mainEsd + "." + subEsd;
					
					return major + "." + minor + "." + maintenance + " ESD#" + esdStr;
				}
			}
			else
			{
				if (maintenance == 0)
					return major + "." + minor + " SP" + servicePack;
				else
					return major + "." + minor + "." + maintenance + " SP" + servicePack;
			}
			
		}
		// 5 digit version number
		else // old version number 15704
		{
			int major       = version                                     / 1000;
			int minor       =(version -  (major * 1000))                  / 100;
			int maintenance =(version - ((major * 1000) + (minor * 100))) / 10;
			int rollup      = version - ((major * 1000) + (minor * 100) + (maintenance * 10));
	
			if (rollup == 0)
				return major + "." + minor + "." + maintenance;
			else
				return major + "." + minor + "." + maintenance + " ESD#" + rollup;
		}
	}

//	public static int versionNumPart(long version, int part)
//	{
//		if (version < Integer.MAX_VALUE)
//		{
//			return FIXME_versionIntPart( (int) version, part );
//		}
//		
//		throw new RuntimeException("versionNumPart() for bigger values than Integer.MAX_VALUE is NOT-YET-IMPLEMENTED");
//	}
	public static int versionNumPart(long version, int part)
	{
		// above 13 digit version num      // long version number (12.5.4 ESD#10.1)  12_05_04_0010_0001   (MM MM MM SSSS PPPP: MM(12)=Major MM(05)=Minor, MM(04)=Maintenance, SSSS(0010)=ServicePack/EsdLevel, PPPP(0001)=PatchLevel/SubEsdLevel)
		if (version > 1_00_00_0000_0000L)  // long version number (15.7   SP100)     15_07_00_0100_0001   (MM MM MM SSSS PPPP: MM(15)=Major MM(07)=Minor, MM(00)=Maintenance, SSSS(0100)=ServicePack/EsdLevel, PPPP(0000)=PatchLevel/SubEsdLevel)
		{                                  // long version number (16.0   SP01 PL01) 16_00_00_0001_0001   (MM MM MM SSSS PPPP: MM(16)=Major MM(00)=Minor, MM(00)=Maintenance, SSSS(0001)=ServicePack/EsdLevel, PPPP(0001)=PatchLevel/SubEsdLevel)
			int baseVer     = (int) (version / 1_0000_0000L); // only save  [12 05 04] get rid of SP/PL [0010 0001] 
			int spPlVer     = (int) (version % 1_0000_0000L); // get rid of [12 05 04] and save   SP/PL [0010 0001]

			int major       = (int) (baseVer / 1_00_00L);
			int minor       = (int) (baseVer % 1_00_00L / 1_00L);
			int maintenance = (int) (baseVer % 1_00L);

			int servicePack = (int) (spPlVer / 1_0000L);
			int patchLevel  = (int) (version % 1_0000L);

//			System.out.println("");
//			System.out.println("versionNumToStr(): version     = " + version);
//			System.out.println("                   baseVer     = " + baseVer);
//			System.out.println("                   spPlVer     = " + spPlVer);
//			System.out.println("                   -----------------------------");
//			System.out.println("                   major       = " + major);
//			System.out.println("                   minor       = " + minor);
//			System.out.println("                   maintenance = " + maintenance);
//			System.out.println("                   servicePack = " + servicePack);
//			System.out.println("                   patchLevel  = " + patchLevel);

			switch (part)
			{
			case VERSION_MAJOR:        return major;
			case VERSION_MINOR:        return minor;
			case VERSION_MAINTENANCE:  return maintenance;
			case VERSION_ROLLUP:       return servicePack;
			case VERSION_SERVICE_PACK: return servicePack;
			case VERSION_PATCH_LEVEL:  return patchLevel;
			default:                   return -1;
			}
		}
		// 9 digit version num           // large version number (12.5.4 ESD#10.1)  125401001   (MMMMSSSPP: MMMM(1254)=MajorMinorMaintenance, SSS(010)=ServicePack/EsdLevel, PP(01)=PatchLevel/SubEsdLevel)
		else if (version > 1000_000_00L) // large version number (15.7   SP100)     157010001   (MMMMSSSPP: MMMM(1570)=MajorMinorMaintenance, SSS(100)=ServicePack/EsdLevel, PP(01)=PatchLevel/SubEsdLevel)
		{                                // large version number (16.0   SP01 PL01) 160000101   (MMMMSSSPP: MMMM(1600)=MajorMinorMaintenance, SSS(001)=ServicePack/EsdLevel, PP(01)=PatchLevel/SubEsdLevel)
			int baseVer     = (int) (version / 1_000_00L);
			int major       = (int) (baseVer / 100L);
			int minor       = (int) (baseVer % 100L / 10L);
			int maintenance = (int) (baseVer % 10L);
			int servicePack = (int) (version % 1_000_00L / 100L);
			int patchLevel  = (int) (version % 100L);

//			System.out.println("");
//			System.out.println("versionNumToStr(): version     = " + version);
//			System.out.println("                   baseVer     = " + baseVer);
//			System.out.println("                   major       = " + major);
//			System.out.println("                   minor       = " + minor);
//			System.out.println("                   maintenance = " + maintenance);
//			System.out.println("                   servicePack = " + servicePack);
//			System.out.println("                   patchLevel  = " + patchLevel);

			switch (part)
			{
			case VERSION_MAJOR:        return major;
			case VERSION_MINOR:        return minor;
			case VERSION_MAINTENANCE:  return maintenance;
			case VERSION_ROLLUP:       return servicePack;
			case VERSION_SERVICE_PACK: return servicePack;
			case VERSION_PATCH_LEVEL:  return patchLevel;
			default:                   return -1;
			}
		}
		// 7 digit version number
		else if (version > 1000_000L) // medium version number 1570100   (major minor maintenance EsdLevel/ServicePack)
		{
			int major       = (int) ( version                                             / 100_000L);
			int minor       = (int) ((version -  (major * 100_000L))                      /  10_000L);
			int maintenance = (int) ((version - ((major * 100_000L) + (minor * 10_000L))) /   1_000L);
			int servicePack = (int) ( version - ((major * 100_000L) + (minor * 10_000L) + (maintenance * 1_000L)));
	
			switch (part)
			{
			case VERSION_MAJOR:        return major;
			case VERSION_MINOR:        return minor;
			case VERSION_MAINTENANCE:  return maintenance;
			case VERSION_ROLLUP:       return servicePack;
			case VERSION_SERVICE_PACK: return servicePack;
			case VERSION_PATCH_LEVEL:  return 0;
			default:                   return -1;
			}
		}
		// 5 digit version number
		else // old version number 15704
		{
			int major       = (int) (  version                                       / 1000L);
			int minor       = (int) ( (version -  (major * 1000L))                   /  100L);
			int maintenance = (int) ( (version - ((major * 1000L) + (minor * 100L))) /   10L);
			int rollup      = (int) (  version - ((major * 1000L) + (minor * 100L) + (maintenance * 10L)) );
	
			switch (part)
			{
			case VERSION_MAJOR:        return major;
			case VERSION_MINOR:        return minor;
			case VERSION_MAINTENANCE:  return maintenance;
			case VERSION_ROLLUP:       return rollup;
			case VERSION_SERVICE_PACK: return rollup;
			case VERSION_PATCH_LEVEL:  return 0;
			default:                   return -1;
			}
		}
	}

	public final static int VERSION_MAJOR        = 1;
	public final static int VERSION_MINOR        = 2;
	public final static int VERSION_MAINTENANCE  = 3;
	public final static int VERSION_ROLLUP       = 4;
	public final static int VERSION_SERVICE_PACK = 5;
	public final static int VERSION_PATCH_LEVEL  = 6;

	/**
	 * If a version part is overflowing it's max value, try to fix this in here
	 * <p>
	 * For example this could be that version '15.0.3.350' has a rollup of 350
	 * and when we want to convert this into a number of: 1503x the 'x' will be to big
	 * so we need to convert this into a 3
	 * <p>
	 * That is the kind of things we should do in here
	 * 
	 * @param type
	 * @param version
	 * @return
	 */
//	private static int fixVersionOverflow(int type, int version)
//	{
//		if (version < 0)
//			return 0;
//
//		if (type == VERSION_ROLLUP)
//		{
//			if (version <  1000) return version;
//			if (version >= 1000) return 999;
//
////			if (version <  10) return version;
////			if (version >= 10) return 9;
//
////			if (version < 10)                       return version;
////			if (version >= 10   && version < 100)   return version / 10;
////			if (version >= 100  && version < 1000)  return version / 100;
////			if (version >= 1000 && version < 10000) return version / 1000;
//		}
//
//		if (type == VERSION_SERVICE_PACK)
//		{
//			if (version <  1000)  return version;
//			if (version >= 1000) return 999;
//		}
//
//		if (type == VERSION_PATCH_LEVEL)
//		{
//			if (version <  100) return version;
//			if (version >= 100) return 99;
//		}
//
//		return version;
//	}
	private static int fixVersionOverflow(int type, int version)
	{
		if (version < 0)
			return 0;

		if (type == VERSION_ROLLUP)
		{
			if (version <  10000) return version;
			if (version >= 10000) return 9999;
		}

		if (type == VERSION_SERVICE_PACK)
		{
			if (version <  10000) return version;
			if (version >= 10000) return 9999;
		}

		if (type == VERSION_PATCH_LEVEL)
		{
			if (version <  10000) return version;
			if (version >= 10000) return 9999;
		}

		return version;
	}
	
	/**
	 * Parses the ASE version string into a number.<br>
	 * The version string will be splitter on the character '/' into different
	 * version parts. The second part will be used as the version string.<br>
	 * 
	 * The version part will then be splitter into different parts by the
	 * delimiter '.'<br>
	 * Four different version parts will be handled:
	 * Major.Minor.Maintenance.Rollup ESD#/ServicePack<br>
	 * Major version part can contain several characters, while the other
	 * version parts can only contain 1 character (only the first character is
	 * used. except for ServicePack where 3 chars is used).
	 * 
	 * @param versionStr
	 *            the ASE version string fetched from the database with select
	 * @@version
	 * @return The version as a number. <br>
	 *         The ase version 12.5            will be returned as 125000000 <br>
	 *         The ase version 12.5.2.0        will be returned as 125200000 <br>
	 *         The ase version 12.5.2.1        will be returned as 125200100 <br>
	 *         The ase version 12.5.4 ESD#10.1 will be returned as 125401001 <br>
	 *         The ase version 15.7 ESD#4      will be returned as 157000400 <br>
	 *         The ase version 15.7 ESD#4.2    will be returned as 157000402 <br>
	 *         The ase version 15.7 SP50       will be returned as 157005000 <br>
	 *         The ase version 15.7 SP100      will be returned as 157010000 <br>
	 *         The ase version 15.7 SP101      will be returned as 157010100 <br>
	 *         The ase version 15.7 SP120      will be returned as 157012000 <br>
	 *         The ase version 16.0            will be returned as 160000000 <br>
	 *         The ase version 16.0 PL01       will be returned as 160000001 <br>
	 *         The ase version 16.0 SP01       will be returned as 160000100 <br>
	 *         The ase version 16.0 SP01 PL01  will be returned as 160000101 <br>
	 */
//------------------------------------------------------------------------
// http://www-dse.oak.sap.corp/cgi-bin/websql/websql.dir/QTS/bugsheet.hts?GO=GO&bugid=751005
//------------------------------------------------------------------------
// ASE 16.0 
// ASE 16.0 PL01
// ASE 16.0 PL02
// ASE 16.0 SP01
// ASE 16.0 SP01 PL01
// ASE 16.0 SP02 
// ASE 16.0 SP02 PL01
// ASE 16.0 SP02 PL02
//------------------------------------------------------------------------
	
// FIXME: this is a ASE-CE version string
//	 Adaptive Server Enterprise/15.0.3/EBF 16748 Cluster Edition/P/x86_64/Enterprise Linux/asepyxis/2837/64-bit/FBO/Mon Jun  1 08:38:39 2009
	public static long sybVersionStringToNumber(String versionStr)
	{
//System.out.println("---> sybVersionStringToNumber(): versionStr='"+versionStr+"'.");
//		long srvVersionNumber = 0;

		int major       = 0; // <12>.5.4
		int minor       = 0; // 12.<5>.4
		int maint       = 0; // 12.5.<4>
		int servicePack = 0; // SP<01> SP<50> SP<100> or ESD#<4>.2
		int patchLevel  = 0; // PL<02> or ESD#4.<2>

		String[] srvVersionParts = versionStr.split("/");
		if (srvVersionParts.length > 0)
		{
			String srvVersionNumberStr = null;
			String aseEsdStr = null;
			String servivePackStr = null;
			String patchLevelStr  = null;
			int    srvVersionNumberStrArrayPos = -1;
			int    aseEsdStrArrayPos           = -1;
//			int    servivePackStrArrayPos      = -1;
//			int    patchLevelStrArrayPos       = -1;

			// Scan the string to see if there are any part that looks like a version str (##.#)
			for (int i=0; i<srvVersionParts.length; i++)
			{
//				if ( srvVersionParts[i].matches("^[0-9][0-9][.][0-9][.][0-9]") && srvVersionNumberStr == null )
//				if ( srvVersionParts[i].matches("^[0-9][0-9][.][0-9]([.][0-9])*") && srvVersionNumberStr == null )
				if ( srvVersionParts[i].matches("^[0-9][0-9][.][0-9].*") && srvVersionNumberStr == null )
				{
					srvVersionNumberStr         = srvVersionParts[i];
					srvVersionNumberStrArrayPos = i;
				}

				// Check for Sybase ESD level
				if ( srvVersionParts[i].indexOf("ESD#") > 0 && aseEsdStr == null)
				{
					aseEsdStr         = srvVersionParts[i];
					aseEsdStrArrayPos = i;
				}

				// Check for "SAP Service Pack, with three numbers SP100, SP110, etc"
				// Check for "SAP Service Pack, with two numbers SP50, SP51, etc"
				if ( srvVersionParts[i].matches(".* SP[0-9].*") && servivePackStr == null)
				{
					servivePackStr         = srvVersionParts[i];
//					servivePackStrArrayPos = i;
				}

				// Check for "SAP Patch Level, with two numbers PL01, PL02, etc", introduced in ASE 16.0
				if ( srvVersionParts[i].matches(".* PL[0-9].*") && patchLevelStr == null)
				{
					patchLevelStr         = srvVersionParts[i];
//					patchLevelStrArrayPos = i;
				}
			}

			if (srvVersionNumberStr == null)
			{
//				_logger.warn("There ASE version string seems to be faulty, can't find any '##.#' in the version number string '" + versionStr + "'.", new Exception("DUMMY EXCEPTION TO GET CALLSTACK"));
				_logger.warn("There ASE version string seems to be faulty, can't find any '##.#' in the version number string '" + versionStr + "'.");
				return 0; // which probably is 0
			}

			String[] srvVersionNumberParts = srvVersionNumberStr.split("\\.");
			if (srvVersionNumberParts.length > 1)
			{
				// Version parts can contain characters...
				// hmm version could be: 12.5.3a
				try
				{
					String versionPart = null;
					// MAJOR version: ( <12>.5.2.1 - MAJOR.minor.maint.rollup )
					if (srvVersionNumberParts.length >= 1)
					{
						versionPart = srvVersionNumberParts[0].trim();
						major = fixVersionOverflow(VERSION_MAJOR, Integer.parseInt(versionPart));
//						srvVersionNumber += 1000 * major;
//						srvVersionNumber += 100000 * major;
					}

					// MINOR version: ( 12.<5>.2.1 - major.MINOR.maint.rollup )
					if (srvVersionNumberParts.length >= 2)
					{
						versionPart = srvVersionNumberParts[1].trim().substring(0, 1);
						minor = fixVersionOverflow(VERSION_MINOR, Integer.parseInt(versionPart));
//						srvVersionNumber += 100 * minor;
//						srvVersionNumber += 10000 * minor;
					}

					// MAINTENANCE version: ( 12.5.<2>.1 - major.minor.MAINT.rollup )
					if (srvVersionNumberParts.length >= 3)
					{
						versionPart = srvVersionNumberParts[2].trim().substring(0, 1);
						maint = fixVersionOverflow(VERSION_MAINTENANCE, Integer.parseInt(versionPart));
//						srvVersionNumber += 10 * maint;
//						srvVersionNumber += 1000 * maint;
					}

					// ROLLUP version: ( 12.5.2.<1> - major.minor.maint.ROLLUP )
					if (srvVersionNumberParts.length >= 4 && srvVersionNumberStrArrayPos != aseEsdStrArrayPos)
					{
//						versionPart = srvVersionNumberParts[3].trim().substring(0, 1);
						versionPart = srvVersionNumberParts[3].trim();
						servicePack = fixVersionOverflow(VERSION_ROLLUP, Integer.parseInt(versionPart));
//						srvVersionNumber += 10 * rollup;
					}
					else // go and check for ESD string, which is another way of specifying ROLLUP
					{
						if (aseEsdStr != null)
						{
							int subEsdStart = -1; // find out ESD# 4.2  (second level)
							int subEsdEnd   = -1;

							int esdStart = aseEsdStr.indexOf("ESD#");
							if (esdStart >= 0)
								esdStart += "ESD#".length();

							// set end to first NON digit (or end of string)
							int esdEnd = esdStart;
							for (; esdEnd<aseEsdStr.length(); esdEnd++)
							{
								if ( ! Character.isDigit(aseEsdStr.charAt(esdEnd)) )
								{
									if ( aseEsdStr.charAt(esdEnd) == '.' )
									{
										subEsdStart = esdEnd + 1;
										subEsdEnd   = subEsdStart;
										for (; subEsdEnd<aseEsdStr.length(); subEsdEnd++)
											if ( ! Character.isDigit(aseEsdStr.charAt(subEsdEnd)) )
												break;
									}
									break;
								}
							}

							if (esdStart != -1)
							{
								try
								{
									versionPart = aseEsdStr.trim().substring(esdStart, esdEnd);
									servicePack = fixVersionOverflow(VERSION_SERVICE_PACK, Integer.parseInt(versionPart));
//									int mainEsdNum = fixVersionOverflow(VERSION_ROLLUP, Integer.parseInt(versionPart));
//									srvVersionNumber += 1 * rollup;
//									srvVersionNumber += 10 * mainEsdNum;
								}
								catch (RuntimeException e) // NumberFormatException,
								{
									_logger.warn("Problems converting some part(s) of the ESD# in the version string '" + srvVersionNumberStr + "' into a number. ESD# string was '"+versionPart+"'. The version number will be set to " + Ver.ver(major, minor, maint, servicePack, patchLevel));
								}
							}
							if (subEsdStart != -1)
							{
								try
								{
									versionPart = aseEsdStr.trim().substring(subEsdStart, subEsdEnd);
									patchLevel = fixVersionOverflow(VERSION_PATCH_LEVEL, Integer.parseInt(versionPart));
//									int subEsdNum = fixVersionOverflow(VERSION_ROLLUP, Integer.parseInt(versionPart));
//									srvVersionNumber += 1 * subEsdNum;
								}
								catch (RuntimeException e) // NumberFormatException,
								{
									_logger.warn("Problems converting some part(s) of the ESD# in the version string '" + srvVersionNumberStr + "' into a number. ESD# string was '"+versionPart+"'. The version number will be set to " + Ver.ver(major, minor, maint, servicePack, patchLevel));
								}
							}
						}
					}
					
					if (servivePackStr != null)
					{
						int start = servivePackStr.indexOf(" SP");
						if (start >= 0)
							start += " SP".length();

						// set end to first NON digit (or end of string)
						int end = start;
						for (; end<servivePackStr.length(); end++)
						{
							if ( ! Character.isDigit(servivePackStr.charAt(end)) )
								break;
						}

						if (start != -1)
						{
							try
							{
								versionPart = servivePackStr.trim().substring(start, end);
								servicePack = fixVersionOverflow(VERSION_SERVICE_PACK, Integer.parseInt(versionPart));
							}
							catch (RuntimeException e) // NumberFormatException,
							{
								_logger.warn("Problems converting some part(s) of the SP (ServicePack) in the version string '" + srvVersionNumberStr + "' into a number. Service Pack string was '"+versionPart+"'. The version number will be set to " + Ver.ver(major, minor, maint, servicePack, patchLevel));
							}
						}
					}
					if (patchLevelStr != null)
					{
						int start = patchLevelStr.indexOf(" PL");
						if (start >= 0)
							start += " PL".length();

						// set end to first NON digit (or end of string)
						int end = start;
						for (; end<patchLevelStr.length(); end++)
						{
							if ( ! Character.isDigit(patchLevelStr.charAt(end)) )
								break;
						}

						if (start != -1)
						{
							try
							{
								versionPart = patchLevelStr.trim().substring(start, end);
								patchLevel = fixVersionOverflow(VERSION_PATCH_LEVEL, Integer.parseInt(versionPart));
//								srvVersionNumber += 1 * patchLevel;
							}
							catch (RuntimeException e) // NumberFormatException,
							{
								_logger.warn("Problems converting some part(s) of the SP (ServicePack) in the version string '" + srvVersionNumberStr + "' into a number. Service Pack string was '"+versionPart+"'. The version number will be set to " + Ver.ver(major, minor, maint, servicePack, patchLevel));
							}
						}
					}
				}
				// catch (NumberFormatException e)
				catch (RuntimeException e) // NumberFormatException,
											// IndexOutOfBoundsException
				{
					_logger.warn("Problems converting some part(s) of the version string '" + srvVersionNumberStr + "' into a number. The version number will be set to " + Ver.ver(major, minor, maint, servicePack, patchLevel));
				}
			}
			else
			{
				_logger.warn("There ASE version string seems to be faulty, can't find any '.' in the version number subsection '" + srvVersionNumberStr + "'.");
			}
		}
		else
		{
			_logger.warn("There ASE version string seems to be faulty, can't find any / in the string '" + versionStr + "'.");
		}

//System.out.println("  <- sybVersionStringToNumber(): <<<--- "+Ver.ver(major, minor, maint, servicePack, patchLevel));
		return Ver.ver(major, minor, maint, servicePack, patchLevel);
		//return srvVersionNumber;
	}



	public static long hanaVersionStringToNumber(String versionStr)
	{
		if (StringUtil.isNullOrBlank(versionStr))
			return 0;

		int major       = 0; // <12>.5.4
		int minor       = 0; // 12.<5>.4
		int maint       = 0; // 12.5.<4>
		int servicePack = 0; // SP<01> SP<50> SP<100> or ESD#<4>.2
		int patchLevel  = 0; // PL<02> or ESD#4.<2>

		String[] sa = versionStr.split("\\.");
		if (sa.length == 5)
		{
			try { major       = Integer.parseInt(sa[0]); } catch(NumberFormatException ex) { major       = 0; _logger.warn("Problem parsing 'major' version string with value '"+sa[0]+"'. Setting this to 0. Caught: "+ex); }
			try { minor       = Integer.parseInt(sa[1]); } catch(NumberFormatException ex) { minor       = 0; _logger.warn("Problem parsing 'minor' version string with value '"+sa[1]+"'. Setting this to 0. Caught: "+ex); }
			      maint       = 0;
			try { servicePack = Integer.parseInt(sa[2]); } catch(NumberFormatException ex) { servicePack = 0; _logger.warn("Problem parsing 'servicePack' version string with value '"+sa[2]+"'. Setting this to 0. Caught: "+ex); }
			try { patchLevel  = Integer.parseInt(sa[3]); } catch(NumberFormatException ex) { patchLevel  = 0; _logger.warn("Problem parsing 'patchLevel' version string with value '"+sa[3]+"'. Setting this to 0. Caught: "+ex); }
			
			return Ver.ver(major, minor, maint, servicePack, patchLevel);
		}
		else
		{
			_logger.error("HANA Version string '"+versionStr+"' doesn't consist of 5 fields separated be '.', can't parse this version string. returning 0");
			return 0;
		}
	}

	public static long asaVersionStringToNumber(String versionStr)
	{
		if (StringUtil.isNullOrBlank(versionStr))
			return 0;

		int major       = 0; // <12>.5.4
		int minor       = 0; // 12.<5>.4
		int maint       = 0; // 12.5.<4>
//		int servicePack = 0; // SP<01> SP<50> SP<100> or ESD#<4>.2
//		int patchLevel  = 0; // PL<02> or ESD#4.<2>
//		int build       = 0; // 16.0.0.<1948> but convert this to a 5 digit number

		String[] sa = versionStr.split("\\.");
		if (sa.length == 4)
		{
			try { major = Integer.parseInt(sa[0]); } catch(NumberFormatException ex) { major  = 0; _logger.warn("Problem parsing 'major' version string with value '"+sa[0]+"'. Setting this to 0. Caught: "+ex); }
			try { minor = Integer.parseInt(sa[1]); } catch(NumberFormatException ex) { minor  = 0; _logger.warn("Problem parsing 'minor' version string with value '"+sa[1]+"'. Setting this to 0. Caught: "+ex); }
			try { maint = Integer.parseInt(sa[2]); } catch(NumberFormatException ex) { maint  = 0; _logger.warn("Problem parsing 'maint' version string with value '"+sa[2]+"'. Setting this to 0. Caught: "+ex); }
//			try { build = Integer.parseInt(sa[3]); } catch(NumberFormatException ex) { build  = 0; _logger.warn("Problem parsing 'build' version string with value '"+sa[3]+"'. Setting this to 0. Caught: "+ex); }
			
			return Ver.ver(major, minor, maint);
		}
		else
		{
			_logger.error("SQL-Anywhere or IQ Version string '"+versionStr+"' doesn't consist of 4 fields separated be '.', can't parse this version string. returning 0");
			return 0;
		}
	}

	public static long iqVersionStringToNumber(String versionStr)
	{
//		IQ:
//		Sybase IQ/16.0.0.656/140812/P/sp04.06/RS6000MP/AIX 6.1.0/64bit/2014-08-12 12:26:08	61	2015-12-01 19:07:31
//		Sybase IQ/15.4.0.3046/141204/P/ESD 5/Enterprise Linux64 - x86_64 - 2.6.18-194.el5/64bit/2014-12-04 17:24:15	1	2015-10-23 09:31:29
//		Sybase IQ/15.4.0.3019/120816/P/ESD 2/Sun_Sparc/OS 5.10/64bit/2012-08-16 12:40:47	3	2015-10-30 18:21:13
//		Sybase IQ/15.2.0.5615/101123/P/ESD 2/Enterprise Linux64 - x86_64 - 2.6.9-67.0.4.ELsmp/64bit/2010-11-23 10:53:30	16	2016-02-05 11:55:59
//		SAP IQ/16.0.102.2.1297/20080/P/sp10.06/MS/Windows 2003/64bit/2015-10-27 01:04:33	2	2016-01-26 16:24:53
//		SAP IQ/16.0.102.1257/20056/P/sp10.03/Enterprise Linux64 - x86_64 - 2.6.18-194.el5/64bit/2015-08-20 17:45:23	2	2016-02-24 14:14:58
//		SAP IQ/16.0.0.809/150928/P/sp08.38/RS6000MP/AIX 6.1.0/64bit/2015-09-28 09:22:15	33	2016-02-29 12:24:36
//		SAP IQ/16.0.0.808/150223/P/sp08.27/MS/Windows 2003/64bit/2015-02-23 16:42:29
//		Summary:
//		if ^sp: unly use 16.0, use sp##.##
//		if ^ESD: use 15.0.0 then ESD#[.#]
				
		int major       = 0; // <15>.4.0
		int minor       = 0; // 15.<4>.0
		int maint       = 0; // 15.4.<0>
		int servicePack = 0; // sp<04>.06 or ESD <4>
		int patchLevel  = 0; // sp04.<06>

		String[] iqVersionParts = versionStr.split("/");
		if (iqVersionParts.length > 0)
		{
			String iqVersionNumberStr = null;
			String iqEsdStr = null;
			String servivePackStr = null;
			String patchLevelStr  = null;

			// Scan the string to see if there are any part that looks like a version str (##.#)
			for (int i=0; i<iqVersionParts.length; i++)
			{
				if ( iqVersionParts[i].matches("^[0-9][0-9][.][0-9].*") && iqVersionNumberStr == null )
				{
					iqVersionNumberStr = iqVersionParts[i];
				}

				// Check for Sybase ESD level
				if ( iqVersionParts[i].indexOf("ESD ") >= 0 && iqEsdStr == null)
				{
					iqEsdStr = iqVersionParts[i];
				}

				// Check for "SAP Service Pack, with three numbers SP100, SP110, etc"
				// Check for "SAP Service Pack, with two numbers SP50, SP51, etc"
				if ( iqVersionParts[i].matches(".*[Ss][Pp][0-9].*") && servivePackStr == null)
				{
					servivePackStr = iqVersionParts[i];
				}

				// Check for "SAP Patch Level, with two numbers PL01, PL02, etc", introduced in ASE 16.0
				if ( iqVersionParts[i].matches(".*[Pp][Ll][0-9].*") && patchLevelStr == null)
				{
					patchLevelStr = iqVersionParts[i];
				}
			}

			if (iqVersionNumberStr == null)
			{
//				_logger.warn("There IQ version string seems to be faulty, can't find any '##.#' in the version number string '" + versionStr + "'.", new Exception("DUMMY EXCEPTION TO GET CALLSTACK"));
				_logger.warn("There IQ version string seems to be faulty, can't find any '##.#' in the version number string '" + versionStr + "'.");
				return 0; // which probably is 0
			}

			String[] srvVersionNumberParts = iqVersionNumberStr.split("\\.");
			if (srvVersionNumberParts.length > 1)
			{
				// Version parts can contain characters...
				// hmm version could be: 12.5.3a
				try
				{
					String versionPart = null;
					// MAJOR version: ( <12>.5.2.1 - MAJOR.minor.maint.rollup )
					if (srvVersionNumberParts.length >= 1)
					{
						versionPart = srvVersionNumberParts[0].trim();
						major = fixVersionOverflow(VERSION_MAJOR, Integer.parseInt(versionPart));
					}

					// MINOR version: ( 12.<5>.2.1 - major.MINOR.maint.rollup )
					if (srvVersionNumberParts.length >= 2)
					{
						versionPart = srvVersionNumberParts[1].trim().substring(0, 1);
						minor = fixVersionOverflow(VERSION_MINOR, Integer.parseInt(versionPart));
					}

//					// MAINTENANCE version: ( 12.5.<2>.1 - major.minor.MAINT.rollup )
//					if (srvVersionNumberParts.length >= 3)
//					{
//						versionPart = srvVersionNumberParts[2].trim().substring(0, 1);
//						maint = fixVersionOverflow(VERSION_MAINTENANCE, Integer.parseInt(versionPart));
//					}
//
//					// ROLLUP version: ( 12.5.2.<1> - major.minor.maint.ROLLUP )
//					if (srvVersionNumberParts.length >= 4 && iqVersionNumberStrArrayPos != iqEsdStrArrayPos)
//					{
////						versionPart = srvVersionNumberParts[3].trim().substring(0, 1);
//						versionPart = srvVersionNumberParts[3].trim();
//						servicePack = fixVersionOverflow(VERSION_ROLLUP, Integer.parseInt(versionPart));
////						srvVersionNumber += 10 * rollup;
//					}
//					else // go and check for ESD string, which is another way of specifying ROLLUP
//					{
						if (iqEsdStr != null)
						{
							int subEsdStart = -1; // find out ESD# 4.2  (second level)
							int subEsdEnd   = -1;

							int esdStart = iqEsdStr.indexOf("ESD ");
							if (esdStart >= 0)
								esdStart += "ESD ".length();

							// set end to first NON digit (or end of string)
							int esdEnd = esdStart;
							for (; esdEnd<iqEsdStr.length(); esdEnd++)
							{
								if ( ! Character.isDigit(iqEsdStr.charAt(esdEnd)) )
								{
									if ( iqEsdStr.charAt(esdEnd) == '.' )
									{
										subEsdStart = esdEnd + 1;
										subEsdEnd   = subEsdStart;
										for (; subEsdEnd<iqEsdStr.length(); subEsdEnd++)
											if ( ! Character.isDigit(iqEsdStr.charAt(subEsdEnd)) )
												break;
									}
									break;
								}
							}

							if (esdStart != -1)
							{
								try
								{
									versionPart = iqEsdStr.trim().substring(esdStart, esdEnd);
									servicePack = fixVersionOverflow(VERSION_SERVICE_PACK, Integer.parseInt(versionPart));
								}
								catch (RuntimeException e) // NumberFormatException,
								{
									_logger.warn("Problems converting some part(s) of the 'ESD ' in the version string '" + iqVersionNumberStr + "' into a number. 'ESD ' string was '"+versionPart+"'. The version number will be set to " + Ver.ver(major, minor, maint, servicePack, patchLevel));
								}
							}
							if (subEsdStart != -1)
							{
								try
								{
									versionPart = iqEsdStr.trim().substring(subEsdStart, subEsdEnd);
									patchLevel = fixVersionOverflow(VERSION_PATCH_LEVEL, Integer.parseInt(versionPart));
								}
								catch (RuntimeException e) // NumberFormatException,
								{
									_logger.warn("Problems converting some part(s) of the 'ESD ' in the version string '" + iqVersionNumberStr + "' into a number. 'ESD ' string was '"+versionPart+"'. The version number will be set to " + Ver.ver(major, minor, maint, servicePack, patchLevel));
								}
							}
						}
//					}
					
					if (servivePackStr != null)
					{
						int start = servivePackStr.indexOf("SP");
						// Forward compatible with SAP version string if they will change it to 'SP## PL##' instead of 'sp##.##'  
						if (start >= 0)
						{
							if (start >= 0)
								start += "SP".length();

							// set end to first NON digit (or end of string)
							int end = start;
							for (; end<servivePackStr.length(); end++)
							{
								if ( ! Character.isDigit(servivePackStr.charAt(end)) )
									break;
							}

							if (start != -1)
							{
								try
								{
									versionPart = servivePackStr.trim().substring(start, end);
									servicePack = fixVersionOverflow(VERSION_SERVICE_PACK, Integer.parseInt(versionPart));
								}
								catch (RuntimeException e) // NumberFormatException,
								{
									_logger.warn("Problems converting some part(s) of the SP (ServicePack) in the version string '" + iqVersionNumberStr + "' into a number. Service Pack string was '"+versionPart+"'. The version number will be set to " + Ver.ver(major, minor, maint, servicePack, patchLevel));
								}
							}
						}
						else if (servivePackStr.indexOf("sp") >= 0) // 'sp##.##'
						{
							String tmp = servivePackStr.replaceAll("[^0-9.]", ""); // remove everything except numbers(0-9) and dot(.)
							String[] sa = tmp.split("\\.");
							if (sa.length >= 1)
								servicePack = fixVersionOverflow(VERSION_SERVICE_PACK, Integer.parseInt(sa[0]));
							if (sa.length >= 2)
								patchLevel = fixVersionOverflow(VERSION_PATCH_LEVEL,   Integer.parseInt(sa[1]));
						}
					}
					if (patchLevelStr != null)
					{
						int start = patchLevelStr.indexOf("PL");
						if (start >= 0)
							start += "PL".length();

						// set end to first NON digit (or end of string)
						int end = start;
						for (; end<patchLevelStr.length(); end++)
						{
							if ( ! Character.isDigit(patchLevelStr.charAt(end)) )
								break;
						}

						if (start != -1)
						{
							try
							{
								versionPart = patchLevelStr.trim().substring(start, end);
								patchLevel = fixVersionOverflow(VERSION_PATCH_LEVEL, Integer.parseInt(versionPart));
//								srvVersionNumber += 1 * patchLevel;
							}
							catch (RuntimeException e) // NumberFormatException,
							{
								_logger.warn("Problems converting some part(s) of the SP (ServicePack) in the version string '" + iqVersionNumberStr + "' into a number. Service Pack string was '"+versionPart+"'. The version number will be set to " + Ver.ver(major, minor, maint, servicePack, patchLevel));
							}
						}
					}
				}
				// catch (NumberFormatException e)
				catch (RuntimeException e) // NumberFormatException,
											// IndexOutOfBoundsException
				{
					_logger.warn("Problems converting some part(s) of the version string '" + iqVersionNumberStr + "' into a number. The version number will be set to " + Ver.ver(major, minor, maint, servicePack, patchLevel));
				}
			}
			else
			{
				_logger.warn("There ASE version string seems to be faulty, can't find any '.' in the version number subsection '" + iqVersionNumberStr + "'.");
			}
		}
		else
		{
			_logger.warn("There ASE version string seems to be faulty, can't find any / in the string '" + versionStr + "'.");
		}

//System.out.println("  <- sybVersionStringToNumber(): <<<--- "+Ver.ver(major, minor, maint, servicePack, patchLevel));
		return Ver.ver(major, minor, maint, servicePack, patchLevel);
	}

	public static long oracleVersionStringToNumber(String versionStr)
	{
		if (StringUtil.isNullOrBlank(versionStr))
			return 0;

		int major       = 0;
		int minor       = 0;
		int maint       = 0;
		int servicePack = 0;
		int patchLevel  = 0;

		String[] sa = versionStr.split("\\.");
		if (sa.length == 5)
		{
			try { major       = Integer.parseInt(sa[0]); } catch(NumberFormatException ex) { major       = 0; _logger.warn("Problem parsing 'major' version string with value '"+sa[0]+"'. Setting this to 0. Caught: "+ex); }
			try { minor       = Integer.parseInt(sa[1]); } catch(NumberFormatException ex) { minor       = 0; _logger.warn("Problem parsing 'minor' version string with value '"+sa[1]+"'. Setting this to 0. Caught: "+ex); }
			try { maint       = Integer.parseInt(sa[2]); } catch(NumberFormatException ex) { maint       = 0; _logger.warn("Problem parsing 'minor' version string with value '"+sa[2]+"'. Setting this to 0. Caught: "+ex); }
			try { servicePack = Integer.parseInt(sa[3]); } catch(NumberFormatException ex) { servicePack = 0; _logger.warn("Problem parsing 'servicePack' version string with value '"+sa[3]+"'. Setting this to 0. Caught: "+ex); }
			try { patchLevel  = Integer.parseInt(sa[4]); } catch(NumberFormatException ex) { patchLevel  = 0; _logger.warn("Problem parsing 'patchLevel' version string with value '"+sa[4]+"'. Setting this to 0. Caught: "+ex); }
			
			return Ver.ver(major, minor, maint, servicePack, patchLevel);
		}
		else
		{
			_logger.error("ORACLE Version string '"+versionStr+"' doesn't consist of 5 fields separated be '.', can't parse this version string. returning 0");
			return 0;
		}
	}

	public static long db2VersionStringToNumber(String versionStr)
	{
		if (StringUtil.isNullOrBlank(versionStr))
			return 0;

		int major       = 0;
		int minor       = 0;
		int maint       = 0;
		int servicePack = 0;
		int patchLevel  = 0;

		String[] sa = versionStr.split("\\.");
		if (sa.length == 4)
		{
			// Remove any starting "DB2 v"
			if (sa[0].startsWith("DB2"))
				sa[0] = sa[0].substring(3).trim();
			if (sa[0].startsWith("v"))
				sa[0] = sa[0].substring(1).trim();

			try { major       = Integer.parseInt(sa[0]); } catch(NumberFormatException ex) { major       = 0; _logger.warn("Problem parsing 'major' version string with value '"+sa[0]+"'. Setting this to 0. Caught: "+ex); }
			try { minor       = Integer.parseInt(sa[1]); } catch(NumberFormatException ex) { minor       = 0; _logger.warn("Problem parsing 'minor' version string with value '"+sa[1]+"'. Setting this to 0. Caught: "+ex); }
			try { maint       = Integer.parseInt(sa[2]); } catch(NumberFormatException ex) { maint       = 0; _logger.warn("Problem parsing 'minor' version string with value '"+sa[2]+"'. Setting this to 0. Caught: "+ex); }
			try { servicePack = Integer.parseInt(sa[3]); } catch(NumberFormatException ex) { servicePack = 0; _logger.warn("Problem parsing 'servicePack' version string with value '"+sa[3]+"'. Setting this to 0. Caught: "+ex); }
//			try { patchLevel  = Integer.parseInt(sa[4]); } catch(NumberFormatException ex) { patchLevel  = 0; _logger.warn("Problem parsing 'patchLevel' version string with value '"+sa[4]+"'. Setting this to 0. Caught: "+ex); }
			
			return Ver.ver(major, minor, maint, servicePack, patchLevel);
		}
		else
		{
			_logger.error("DB2 Version string '"+versionStr+"' doesn't consist of 4 fields separated be '.', can't parse this version string. returning 0");
			return 0;
		}
	}
	
	public static long sqlServerVersionStringToNumber(String versionStr)
	{
		//System.out.println("---> sybVersionStringToNumber(): versionStr='"+versionStr+"'.");
//		long srvVersionNumber = 0;

		if (StringUtil.isNullOrBlank(versionStr))
			return 0;
		
		int major       = 0; // <12>.5.4
		int minor       = 0; // 12.<5>.4
		int maint       = 0; // 12.5.<4>
		int servicePack = 0; // SP<01> SP<50> SP<100> or ESD#<4>.2
		int patchLevel  = 0; // PL<02> or ESD#4.<2>

		if (versionStr.startsWith("Microsoft SQL Server "))
		{
			// Remove "Microsoft SQL Server "
			versionStr = versionStr.substring("Microsoft SQL Server ".length());
			
			// for example... spit at ' - '
			// [0] = Microsoft SQL Server 2017 (RTM-CU9) (KB4341265)
			// [1] = 14.0.3030.27 (X64) Jun 29 2018 18:02:47 Copyright (C) 2017 Microsoft Corporation Standard Edition (64-bit) on Windows Server 2016 Standard 10.0 (Build 14393: ) (Hypervisor)	
			String[] srvVersionParts = versionStr.split(" - ");
			if (srvVersionParts.length >= 2)
			{
				String verStrP1 = srvVersionParts[0].trim();
			//	String verStrP2 = srvVersionParts[1].trim(); 

				int firstSpace = verStrP1.indexOf(' ');
				String prodYear = verStrP1.substring(0, firstSpace == -1 ? 4 : firstSpace);
				major = StringUtil.parseInt(prodYear, -1);
				
				if (verStrP1.startsWith("2008 R2"))
					minor = 2;
				
				int firstLP = verStrP1.indexOf('(');
				int firstRP = verStrP1.indexOf(')');
				if (firstLP >= 0 && firstRP >= 0)
				{
					String subVerStr = verStrP1.substring(firstLP+1, firstRP);
	
					int spPosStart = subVerStr.indexOf("SP"); 
					if (spPosStart >= 0)
					{
						spPosStart += 2;
						int spPosEnd = spPosStart;
						for(; spPosEnd<subVerStr.length() && Character.isDigit(subVerStr.charAt(spPosEnd)); spPosEnd++)
							/*do nothing, just incrementing: spPosEnd*/;
						servicePack = StringUtil.parseInt(subVerStr.substring(spPosStart, spPosEnd), -1);
					}

					int cuPosStart = subVerStr.indexOf("CU"); 
					if (cuPosStart >= 0)
					{
						cuPosStart += 2;
						int cuPosEnd = cuPosStart;
						for(; cuPosEnd<subVerStr.length() && Character.isDigit(subVerStr.charAt(cuPosEnd)); cuPosEnd++)
							/*do nothing, just incrementing: cuPosEnd*/;
						patchLevel = StringUtil.parseInt(subVerStr.substring(cuPosStart, cuPosEnd), -1);
					}
				}
				//System.out.println("major="+major+", minor="+minor+", sp="+servicePack+", cu="+patchLevel);
			}
		}
		else
		{
			_logger.warn("There SQL-Server version string seems to be faulty, can't find 'Microsoft SQL Server' in the string '" + versionStr + "'.");
			return 0;
		}

		//System.out.println("  <- sqlServerVersionStringToNumber(): <<<--- "+Ver.ver(major, minor, maint, servicePack, patchLevel));
		return Ver.ver(major, minor, maint, servicePack, patchLevel);
		
	}
	
	/**
	 * Take a "short version int" into a "long version int", which is the "internal" version number used in DbxTune to compare version numbers.<br>
	 * <b>Note</b>: this might change in the future to use an larger part of the Integer, so we can compare larger minor/mantenance numbers. (today we can only have 1 minor/maintenance digit)<br>
	 * So use the methods: <code>ver(major); ver(major,minor); ver(major,minor,maint); ver(major,minor,maint, sp); ver(major,minor,maint, sp, pl);</code> to generate version numbers for comparisons.   
	 * <pre>
	 * ShortVerStr    ShortInt(6)   Long(14)
	 * -----------    ------        ------------------
	 *       1.2.3 ->  10203    ->   1_02_03 0000 0000    
	 *      99.2.3 -> 990203    ->  99_02_03_0000_0000
	 * </pre>
	 * @param shortVerInt
	 * @return a "long version number" which can be used for comparison inside DbxTune
	 */
	public static long shortVersionStringToNumber(int shortVerInt)
	{
		int major       =  shortVerInt / 10_000;
		int minor       = (shortVerInt % 10_000) / 100;
		int maint       =  shortVerInt % 100;

		if (major > 99) _logger.warn("shortVersionStringToNumber(shortVerInt="+shortVerInt+"): long version string can't handle 'major' greater than 99 (2 digit), the passed shortVerInt="+shortVerInt+" was calculated as major="+major);
		if (minor > 99) _logger.warn("shortVersionStringToNumber(shortVerInt="+shortVerInt+"): long version string can't handle 'minor' greater than 99 (2 digit), the passed shortVerInt="+shortVerInt+" was calculated as minor="+minor);
		if (maint > 99) _logger.warn("shortVersionStringToNumber(shortVerInt="+shortVerInt+"): long version string can't handle 'maint' greater than 99 (2 digit), the passed shortVerInt="+shortVerInt+" was calculated as maint="+maint);

		return ver(major, minor, maint);
	}



	//////////////////////////////////////////////
	// Some small TEST CODE
	//////////////////////////////////////////////
	public static void main(String[] args)
	{
		long version = 0;
//		version = 12_05_00_0003_0000L; System.out.println(version + " = "+ versionNumToStr(version));
//		version = 12_05_04_0001_0000L; System.out.println(version + " = "+ versionNumToStr(version));
//		version = 12_05_04_0010_0002L; System.out.println(version + " = "+ versionNumToStr(version));
//		version = 15_07_00_0004_0020L; System.out.println(version + " = "+ versionNumToStr(version));
//		version = 15_07_00_0000_0000L; System.out.println(version + " = "+ versionNumToStr(version));
//		version = 15_07_00_0100_0000L; System.out.println(version + " = "+ versionNumToStr(version));
//		version = 15_07_00_0102_0000L; System.out.println(version + " = "+ versionNumToStr(version));
//		version = 15_07_00_0150_0000L; System.out.println(version + " = "+ versionNumToStr(version));
//		version = 15_07_00_0160_0000L; System.out.println(version + " = "+ versionNumToStr(version));
//		version = 15_07_00_0050_0000L; System.out.println(version + " = "+ versionNumToStr(version));
//		version = 15_07_00_0051_0000L; System.out.println(version + " = "+ versionNumToStr(version));
//		version = 15_07_00_0060_0000L; System.out.println(version + " = "+ versionNumToStr(version));
//		version = 16_00_00_0000_0000L; System.out.println(version + " = "+ versionNumToStr(version));
//		version = 16_00_00_0000_0001L; System.out.println(version + " = "+ versionNumToStr(version));
//		version = 16_00_00_0001_0000L; System.out.println(version + " = "+ versionNumToStr(version));
//		version = 16_00_00_0001_0001L; System.out.println(version + " = "+ versionNumToStr(version));
//		version = 16_01_02_0001_0001L; System.out.println(version + " = "+ versionNumToStr(version));
//		version = 99_99_99_9999_9999L; System.out.println(version + " = "+ versionNumToStr(version));
//		System.out.println("----------------------------------------------------------------------------------");

		version = 1250_003_00; System.out.println(version + " = "+ versionNumToStr(version));
		version = 1254_001_00; System.out.println(version + " = "+ versionNumToStr(version));
		version = 1254_010_02; System.out.println(version + " = "+ versionNumToStr(version));
		version = 1570_004_20; System.out.println(version + " = "+ versionNumToStr(version));
		version = 1570_000_00; System.out.println(version + " = "+ versionNumToStr(version));
		version = 1570_100_00; System.out.println(version + " = "+ versionNumToStr(version));
		version = 1570_102_00; System.out.println(version + " = "+ versionNumToStr(version));
		version = 1570_150_00; System.out.println(version + " = "+ versionNumToStr(version));
		version = 1570_160_00; System.out.println(version + " = "+ versionNumToStr(version));
		version = 1570_050_00; System.out.println(version + " = "+ versionNumToStr(version));
		version = 1570_051_00; System.out.println(version + " = "+ versionNumToStr(version));
		version = 1570_060_00; System.out.println(version + " = "+ versionNumToStr(version));
		version = 1600_000_00; System.out.println(version + " = "+ versionNumToStr(version));
		version = 1600_000_01; System.out.println(version + " = "+ versionNumToStr(version));
		version = 1600_001_00; System.out.println(version + " = "+ versionNumToStr(version));
		version = 1600_001_01; System.out.println(version + " = "+ versionNumToStr(version));
		version = 1612_001_01; System.out.println(version + " = "+ versionNumToStr(version));
		version = 9999_999_99; System.out.println(version + " = "+ versionNumToStr(version));
		System.out.println("----------------------------------------------------------------------------------");

		version = Ver.ver(12,5,4, 10,  2); System.out.println(version + " = "+ versionNumToStr(version));
		version = Ver.ver(15,7,0, 100   ); System.out.println(version + " = "+ versionNumToStr(version));
		version = Ver.ver(15,7,0, 101   ); System.out.println(version + " = "+ versionNumToStr(version));
		version = Ver.ver(16,0          ); System.out.println(version + " = "+ versionNumToStr(version));
		version = Ver.ver(16,0,0, 0,   1); System.out.println(version + " = "+ versionNumToStr(version));
		version = Ver.ver(16,0,0, 1     ); System.out.println(version + " = "+ versionNumToStr(version));
		version = Ver.ver(16,0,0, 1,   1); System.out.println(version + " = "+ versionNumToStr(version));
		System.out.println("----------------------------------------------------------------------------------");

		version = Ver.ver(12,5,4, 10,  2); System.out.println(version + " = "+ versionNumToStr(version));
		version = Ver.ver(15,7,0, 51,  0); System.out.println(version + " = "+ versionNumToStr(version));
		version = Ver.ver(15,7,0, 101, 0); System.out.println(version + " = "+ versionNumToStr(version));
		version = Ver.ver(44,5,6, 777,88); System.out.println(version + " = "+ versionNumToStr(version));
//		version = Ver.ver(1, 2,3, 4,   5); System.out.println(version + " = "+ versionNumToStr(version)); // should fail... do to main<10
		System.out.println("----------------------------------------------------------------------------------");

		version = 1250_030; System.out.println(version + " = "+ versionNumToStr(version));
		version = 1254_010; System.out.println(version + " = "+ versionNumToStr(version));
		version = 1570_042; System.out.println(version + " = "+ versionNumToStr(version));
		version = 1570_000; System.out.println(version + " = "+ versionNumToStr(version));
		version = 1570_100; System.out.println(version + " = "+ versionNumToStr(version));
		version = 1570_120; System.out.println(version + " = "+ versionNumToStr(version));
		version = 1570_150; System.out.println(version + " = "+ versionNumToStr(version));
		version = 1570_160; System.out.println(version + " = "+ versionNumToStr(version));
		version = 1570_050; System.out.println(version + " = "+ versionNumToStr(version));
		version = 1570_051; System.out.println(version + " = "+ versionNumToStr(version));
		version = 1570_060; System.out.println(version + " = "+ versionNumToStr(version));
		System.out.println("----------------------------------------------------------------------------------");

//		testVersion(1250011, "12.5.0 ESD#1.1");
//		testVersion(1250030, "Adaptive Server Enterprise/12.5.0.3/EBF 11449 ESD#4/...");
////		testVersion(1250090, "Adaptive Server Enterprise/12.5.0.10/P/x86_64/...");
//		testVersion(1254010, "Adaptive Server Enterprise/12.5.4/EBF 16748 SMP ESD#1 /P/x86_64/...");
//		testVersion(1254090, "Adaptive Server Enterprise/12.5.4/EBF 16748 SMP ESD#11/P/x86_64/...");
//		
//		testVersion(1550015, "15.5.0 ESD#1.5");
//		testVersion(1502050, "Adaptive Server Enterprise/15.0.2/EBF 16748 SMP ESD#5 /P/x86_64/...");
//		testVersion(1503040, "Adaptive Server Enterprise/15.0.3/EBF 16748 SMP ESD#4 /P/x86_64/...");
//		testVersion(1503042, "Adaptive Server Enterprise/15.0.3/EBF 16748 SMP ESD#4.2 /P/x86_64/...");
//		
//		testVersion(1570040, "Adaptive Server Enterprise/15.7.0/EBF 16748 SMP ESD#4 /P/x86_64/...");
//		testVersion(1570042, "Adaptive Server Enterprise/15.7.0/EBF 16748 SMP ESD#4.2 /P/x86_64/...");
//
//		testVersion(1570100, "Adaptive Server Enterprise/15.7/EBF 16748 SMP SP100 /P/x86_64/...");   
//		testVersion(1570101, "Adaptive Server Enterprise/15.7/EBF 16748 SMP SP101 /P/x86_64/...");   
//		testVersion(1570111, "Adaptive Server Enterprise/15.7/EBF 16748 SMP SP111 /P/x86_64/...");   
//		testVersion(1570200, "Adaptive Server Enterprise/15.7/EBF 16748 SMP SP200 /P/x86_64/...");   
//		testVersion(1570999, "Adaptive Server Enterprise/15.7/EBF 16748 SMP SP2000 /P/x86_64/...");  
//		testVersion(1571100, "Adaptive Server Enterprise/15.7.1/EBF 16748 SMP SP100 /P/x86_64/...");   
//
//		testVersion(1570050, "Adaptive Server Enterprise/15.7.0/EBF 21207 SMP SP50 /P/Solaris AMD64/...");
//		testVersion(1570051, "Adaptive Server Enterprise/15.7.0/EBF 21757 SMP SP51 /P/x86_64/Enterprise Linux/...");

		testVersion(Ver.ver(12,5,0,1,1), "12.5.0 ESD#1.1");
		testVersion(Ver.ver(12,5,0,3),   "Adaptive Server Enterprise/12.5.0.3/EBF XXXX ESD#4/...");
		testVersion(Ver.ver(12,5,0,10),  "Adaptive Server Enterprise/12.5.0.10/...");
		testVersion(Ver.ver(12,5,4,1),   "Adaptive Server Enterprise/12.5.4/EBF XXXX SMP ESD#1 /...");
		testVersion(Ver.ver(12,5,4,11),  "Adaptive Server Enterprise/12.5.4/EBF XXXX SMP ESD#11/...");
		
		testVersion(Ver.ver(15,5,0,1,5), "15.5.0 ESD#1.5");
		testVersion(Ver.ver(15,0,2,5),   "Adaptive Server Enterprise/15.0.2/EBF XXXX SMP ESD#5 /...");
		testVersion(Ver.ver(15,0,3,4),   "Adaptive Server Enterprise/15.0.3/EBF XXXX SMP ESD#4 /...");
		testVersion(Ver.ver(15,0,3,4,2), "Adaptive Server Enterprise/15.0.3/EBF XXXX SMP ESD#4.2 /...");
		
		testVersion(Ver.ver(15,7,0,4),   "Adaptive Server Enterprise/15.7.0/EBF XXXX SMP ESD#4 /...");
		testVersion(Ver.ver(15,7,0,4,2), "Adaptive Server Enterprise/15.7.0/EBF XXXX SMP ESD#4.2 /...");

		testVersion(Ver.ver(15,7,0,100), "Adaptive Server Enterprise/15.7/EBF XXXX SMP SP100 /...");   
		testVersion(Ver.ver(15,7,0,101), "Adaptive Server Enterprise/15.7/EBF XXXX SMP SP101 /...");   
		testVersion(Ver.ver(15,7,0,111), "Adaptive Server Enterprise/15.7/EBF XXXX SMP SP111 /...");   
		testVersion(Ver.ver(15,7,0,200), "Adaptive Server Enterprise/15.7/EBF XXXX SMP SP200 /...");   
		testVersion(Ver.ver(15,7,0,2000),"Adaptive Server Enterprise/15.7/EBF XXXX SMP SP2000 /...");  
		testVersion(Ver.ver(15,7,1,100), "Adaptive Server Enterprise/15.7.1/EBF XXXX SMP SP100 /...");   

		testVersion(Ver.ver(15,7,0,50),  "Adaptive Server Enterprise/15.7.0/EBF XXXX SMP SP50 /...");
		testVersion(Ver.ver(15,7,0,51),  "Adaptive Server Enterprise/15.7.0/EBF XXXX SMP SP51 /...");

		testVersion(Ver.ver(16,0),       "Adaptive Server Enterprise/16.0/EBF XXXX SMP /...");
		testVersion(Ver.ver(16,0,0,0,1), "Adaptive Server Enterprise/16.0/EBF XXXX SMP PL1 /...");
		testVersion(Ver.ver(16,0,0,0,1), "Adaptive Server Enterprise/16.0/EBF XXXX SMP PL01 /...");
		testVersion(Ver.ver(16,0,0,1),   "Adaptive Server Enterprise/16.0/EBF XXXX SMP SP1 /...");
		testVersion(Ver.ver(16,0,0,1),   "Adaptive Server Enterprise/16.0/EBF XXXX SMP SP01 /...");
		testVersion(Ver.ver(16,0,0,1,1), "Adaptive Server Enterprise/16.0/EBF XXXX SMP SP1 PL1/...");
		testVersion(Ver.ver(16,0,0,1,1), "Adaptive Server Enterprise/16.0/EBF XXXX SMP SP01 PL01/...");
		testVersion(Ver.ver(16,0,1,1,1), "Adaptive Server Enterprise/16.0.1/EBF XXXX SMP SP1 PL1/...");
		testVersion(Ver.ver(16,0,1,1,1), "Adaptive Server Enterprise/16.0.1/EBF XXXX SMP SP01 PL01/...");
		testVersion(Ver.ver(16,1,0,1,1), "Adaptive Server Enterprise/16.1/EBF XXXX SMP SP01 PL01/...");

		// Check version with another format
		testVersion(Ver.ver(16,0),       "Adaptive Server Enterprise/16.0/EBF XXXX SMP /...");
		testVersion(Ver.ver(16,0,0,0,1), "Adaptive Server Enterprise/16.0 PL1/EBF XXXX SMP /...");
		testVersion(Ver.ver(16,0,0,0,1), "Adaptive Server Enterprise/16.0 PL01 /EBF XXXX SMP /...");
		testVersion(Ver.ver(16,0,0,1),   "Adaptive Server Enterprise/16.0 SP1 /EBF XXXX SMP /...");
		testVersion(Ver.ver(16,0,0,1),   "Adaptive Server Enterprise/16.0 SP01/EBF XXXX SMP /...");
		testVersion(Ver.ver(16,0,0,1,1), "Adaptive Server Enterprise/16.0 SP1 PL1/EBF XXXX SMP/...");
		testVersion(Ver.ver(16,0,0,1,1), "Adaptive Server Enterprise/16.0 SP01 PL01/EBF XXXX SMP /...");
		testVersion(Ver.ver(16,0,1,1,1), "Adaptive Server Enterprise/16.0.1 SP1 PL1/EBF XXXX SMP /...");
		testVersion(Ver.ver(16,0,1,1,1), "Adaptive Server Enterprise/16.0.1 SP01 PL01/EBF XXXX SMP /...");
		testVersion(Ver.ver(16,1,0,1,1), "Adaptive Server Enterprise/16.1 SP01 PL01/EBF XXXX SMP /...");

		// Some real life version strings for 16
		testVersion(Ver.ver(16,0,0,0,0), "Adaptive Server Enterprise/16.0/EBF 22385 SMP/P/X64/Windows Server/asecepheus/3530/64-bit/FBO/Sun Feb 16 06:52:50 2014");
		testVersion(Ver.ver(16,0,0,0,1), "Adaptive Server Enterprise/16.0 GA PL01/EBF 22544 SMP/P/x86_64/Enterprise Linux/ase160sp00pl01/3523/64-bit/FBO/Tue Apr 15 13:24:31 2014");

		
		
		// Some IQ real life
		testIqVersion(Ver.ver(16,0,0, 4, 6), "Sybase IQ/16.0.0.656/140812/P/sp04.06/RS6000MP/AIX 6.1.0/64bit/2014-08-12 12:26:08");
		testIqVersion(Ver.ver(15,4,0, 5, 0), "Sybase IQ/15.4.0.3046/141204/P/ESD 5/Enterprise Linux64 - x86_64 - 2.6.18-194.el5/64bit/2014-12-04 17:24:15");
		testIqVersion(Ver.ver(15,4,0, 2, 0), "Sybase IQ/15.4.0.3019/120816/P/ESD 2/Sun_Sparc/OS 5.10/64bit/2012-08-16 12:40:47");
		testIqVersion(Ver.ver(15,2,0, 2, 0), "Sybase IQ/15.2.0.5615/101123/P/ESD 2/Enterprise Linux64 - x86_64 - 2.6.9-67.0.4.ELsmp/64bit/2010-11-23 10:53:30");
		testIqVersion(Ver.ver(16,0,0, 10,6), "SAP IQ/16.0.102.2.1297/20080/P/sp10.06/MS/Windows 2003/64bit/2015-10-27 01:04:33");
		testIqVersion(Ver.ver(16,0,0, 10,3), "SAP IQ/16.0.102.1257/20056/P/sp10.03/Enterprise Linux64 - x86_64 - 2.6.18-194.el5/64bit/2015-08-20 17:45:23");
		testIqVersion(Ver.ver(16,0,0, 8,38), "SAP IQ/16.0.0.809/150928/P/sp08.38/RS6000MP/AIX 6.1.0/64bit/2015-09-28 09:22:15");
		testIqVersion(Ver.ver(16,0,0, 8,27), "SAP IQ/16.0.0.808/150223/P/sp08.27/MS/Windows 2003/64bit/2015-02-23 16:42:29");

		// Some IQ Made up in case of SAP decides to go: SP PL
		testIqVersion(Ver.ver(16,1,0, 1, 0), "SAP IQ/16.1.102.2.1297/20080/P/SP01/MS/Windows 2003/64bit/2015-10-27 01:04:33");
		testIqVersion(Ver.ver(16,1,0, 20,1), "SAP IQ/16.1.102.2.1297/20080/P/SP20 PL01/MS/Windows 2003/64bit/2015-10-27 01:04:33");
		testIqVersion(Ver.ver(16,1,0, 1, 0), "SAP IQ/16.1.102.2.1297/20080/P/xxx SP01/MS/Windows 2003/64bit/2015-10-27 01:04:33");
		testIqVersion(Ver.ver(16,1,0, 20,1), "SAP IQ/16.1.102.2.1297/20080/P/xxx SP20 PL01/MS/Windows 2003/64bit/2015-10-27 01:04:33");

		// Some IQ Made up in case of SAP decides to go: SP PL and also removes everything after 16.1  16.1<.102.2.1297> 
		testIqVersion(Ver.ver(16,1,0, 1, 0), "SAP IQ/16.1/20080/P/SP01/MS/...");
		testIqVersion(Ver.ver(16,1,0, 20,1), "SAP IQ/16.1/20080/P/SP20 PL01/MS/...");
		testIqVersion(Ver.ver(16,1,0, 1, 0), "SAP IQ/16.1/20080/P/xxx SP01/MS/...");
		testIqVersion(Ver.ver(16,1,0, 20,1), "SAP IQ/16.1/20080/P/xxx SP20 PL01/MS/...");
		// Some IQ Made up in case of SAP decides to go: 16.1 SP## PL## like it's in ASE
		testIqVersion(Ver.ver(16,1,0, 1, 0), "SAP IQ/16.1 SP01/20080/P/MS/...");
		testIqVersion(Ver.ver(16,1,0, 0, 1), "SAP IQ/16.1 PL01/20080/P/MS/...");
		testIqVersion(Ver.ver(16,1,0, 20,1), "SAP IQ/16.1 SP20 PL01/20080/P/MS/...");
		
		
		testDb2Version(Ver.ver(11,1,2, 2), "DB2 v11.1.2.2");
	}

	private static boolean testVersion(long expectedNumVer, String verStr)
	{
		long version = sybVersionStringToNumber(verStr);
		
		if (version != expectedNumVer) 
		{
			System.out.println("FAILED: version="+version+", expectedVersion="+expectedNumVer+", VersionStr='"+verStr+"'."); 
			return false;
		}
		else 
		{
			System.out.println("OK    : version="+version+", expectedVersion="+expectedNumVer+", VersionStr='"+verStr+"'."); 
			return true;
		}
	}

	private static boolean testIqVersion(long expectedNumVer, String verStr)
	{
		long version = iqVersionStringToNumber(verStr);
		
		if (version != expectedNumVer) 
		{
			System.out.println("FAILED: version="+version+", expectedVersion="+expectedNumVer+", VersionStr='"+verStr+"'."); 
			return false;
		}
		else 
		{
			System.out.println("OK    : version="+version+", expectedVersion="+expectedNumVer+", VersionStr='"+verStr+"'."); 
			return true;
		}
	}

	private static boolean testDb2Version(long expectedNumVer, String verStr)
	{
		long version = db2VersionStringToNumber(verStr);
		
		if (version != expectedNumVer) 
		{
			System.out.println("FAILED: version="+version+", expectedVersion="+expectedNumVer+", VersionStr='"+verStr+"'."); 
			return false;
		}
		else 
		{
			System.out.println("OK    : version="+version+", expectedVersion="+expectedNumVer+", VersionStr='"+verStr+"'."); 
			return true;
		}
	}
}
