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

	
	
	
	
	
	/** 
	 * Convert a int version to a string version 
	 * <p>
	 * <code>15030 will be "15.0.3"</code>
	 * <code>15031 will be "15.0.3 ESD#1"</code>
	 */
	public static String versionIntToStr(int version)
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
//            System.out.println("versionIntToStr(): version     = " + version);
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

	public static int versionIntPart(int version, int part)
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
//            System.out.println("versionIntToStr(): version     = " + version);
//            System.out.println("                   baseVer     = " + baseVer);
//            System.out.println("                   major       = " + major);
//            System.out.println("                   minor       = " + minor);
//            System.out.println("                   maintenance = " + maintenance);
//            System.out.println("                   servicePack = " + servicePack);
//            System.out.println("                   patchLevel  = " + patchLevel);

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
		else if (version > 1000000) // medium version number 1570100   (major minor maintenance EsdLevel/ServicePack)
		{
			int major       = version                                         / 100000;
			int minor       =(version -  (major * 100000))                    / 10000;
			int maintenance =(version - ((major * 100000) + (minor * 10000))) / 1000;
			int servicePack = version - ((major * 100000) + (minor * 10000) + (maintenance * 1000));
	
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
			int major       = version                                     / 1000;
			int minor       =(version -  (major * 1000))                  / 100;
			int maintenance =(version - ((major * 1000) + (minor * 100))) / 10;
			int rollup      = version - ((major * 1000) + (minor * 100) + (maintenance * 10));
	
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
	private static int fixVersionOverflow(int type, int version)
	{
		if (version < 0)
			return 0;

		if (type == VERSION_ROLLUP)
		{
			if (version <  1000) return version;
			if (version >= 1000) return 999;

//			if (version <  10) return version;
//			if (version >= 10) return 9;

//			if (version < 10)                       return version;
//			if (version >= 10   && version < 100)   return version / 10;
//			if (version >= 100  && version < 1000)  return version / 100;
//			if (version >= 1000 && version < 10000) return version / 1000;
		}

		if (type == VERSION_SERVICE_PACK)
		{
			if (version <  1000)  return version;
			if (version >= 1000) return 999;
		}

		if (type == VERSION_PATCH_LEVEL)
		{
			if (version <  100) return version;
			if (version >= 100) return 99;
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
	public static int sybVersionStringToNumber(String versionStr)
	{
//System.out.println("---> sybVersionStringToNumber(): versionStr='"+versionStr+"'.");
//		int aseVersionNumber = 0;

		int major       = 0; // <12>.5.4
		int minor       = 0; // 12.<5>.4
		int maint       = 0; // 12.5.<4>
		int servicePack = 0; // SP<01> SP<50> SP<100> or ESD#<4>.2
		int patchLevel  = 0; // PL<02> or ESD#4.<2>

		String[] aseVersionParts = versionStr.split("/");
		if (aseVersionParts.length > 0)
		{
			String aseVersionNumberStr = null;
			String aseEsdStr = null;
			String servivePackStr = null;
			String patchLevelStr  = null;
			int    aseVersionNumberStrArrayPos = -1;
			int    aseEsdStrArrayPos           = -1;
//			int    servivePackStrArrayPos      = -1;
//			int    patchLevelStrArrayPos       = -1;

			// Scan the string to see if there are any part that looks like a version str (##.#)
			for (int i=0; i<aseVersionParts.length; i++)
			{
//				if ( aseVersionParts[i].matches("^[0-9][0-9][.][0-9][.][0-9]") && aseVersionNumberStr == null )
//				if ( aseVersionParts[i].matches("^[0-9][0-9][.][0-9]([.][0-9])*") && aseVersionNumberStr == null )
				if ( aseVersionParts[i].matches("^[0-9][0-9][.][0-9].*") && aseVersionNumberStr == null )
				{
					aseVersionNumberStr         = aseVersionParts[i];
					aseVersionNumberStrArrayPos = i;
				}

				// Check for Sybase ESD level
				if ( aseVersionParts[i].indexOf("ESD#") > 0 && aseEsdStr == null)
				{
					aseEsdStr         = aseVersionParts[i];
					aseEsdStrArrayPos = i;
				}

				// Check for "SAP Service Pack, with three numbers SP100, SP110, etc"
				// Check for "SAP Service Pack, with two numbers SP50, SP51, etc"
				if ( aseVersionParts[i].matches(".* SP[0-9].*") && servivePackStr == null)
				{
					servivePackStr         = aseVersionParts[i];
//					servivePackStrArrayPos = i;
				}

				// Check for "SAP Patch Level, with two numbers PL01, PL02, etc", introduced in ASE 16.0
				if ( aseVersionParts[i].matches(".* PL[0-9].*") && patchLevelStr == null)
				{
					patchLevelStr         = aseVersionParts[i];
//					patchLevelStrArrayPos = i;
				}
			}

			if (aseVersionNumberStr == null)
			{
//				_logger.warn("There ASE version string seems to be faulty, can't find any '##.#' in the version number string '" + versionStr + "'.", new Exception("DUMMY EXCEPTION TO GET CALLSTACK"));
				_logger.warn("There ASE version string seems to be faulty, can't find any '##.#' in the version number string '" + versionStr + "'.");
				return 0; // which probably is 0
			}

			String[] aseVersionNumberParts = aseVersionNumberStr.split("\\.");
			if (aseVersionNumberParts.length > 1)
			{
				// Version parts can contain characters...
				// hmm version could be: 12.5.3a
				try
				{
					String versionPart = null;
					// MAJOR version: ( <12>.5.2.1 - MAJOR.minor.maint.rollup )
					if (aseVersionNumberParts.length >= 1)
					{
						versionPart = aseVersionNumberParts[0].trim();
						major = fixVersionOverflow(VERSION_MAJOR, Integer.parseInt(versionPart));
//						aseVersionNumber += 1000 * major;
//						aseVersionNumber += 100000 * major;
					}

					// MINOR version: ( 12.<5>.2.1 - major.MINOR.maint.rollup )
					if (aseVersionNumberParts.length >= 2)
					{
						versionPart = aseVersionNumberParts[1].trim().substring(0, 1);
						minor = fixVersionOverflow(VERSION_MINOR, Integer.parseInt(versionPart));
//						aseVersionNumber += 100 * minor;
//						aseVersionNumber += 10000 * minor;
					}

					// MAINTENANCE version: ( 12.5.<2>.1 - major.minor.MAINT.rollup )
					if (aseVersionNumberParts.length >= 3)
					{
						versionPart = aseVersionNumberParts[2].trim().substring(0, 1);
						maint = fixVersionOverflow(VERSION_MAINTENANCE, Integer.parseInt(versionPart));
//						aseVersionNumber += 10 * maint;
//						aseVersionNumber += 1000 * maint;
					}

					// ROLLUP version: ( 12.5.2.<1> - major.minor.maint.ROLLUP )
					if (aseVersionNumberParts.length >= 4 && aseVersionNumberStrArrayPos != aseEsdStrArrayPos)
					{
//						versionPart = aseVersionNumberParts[3].trim().substring(0, 1);
						versionPart = aseVersionNumberParts[3].trim();
						servicePack = fixVersionOverflow(VERSION_ROLLUP, Integer.parseInt(versionPart));
//						aseVersionNumber += 10 * rollup;
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
//									aseVersionNumber += 1 * rollup;
//									aseVersionNumber += 10 * mainEsdNum;
								}
								catch (RuntimeException e) // NumberFormatException,
								{
									_logger.warn("Problems converting some part(s) of the ESD# in the version string '" + aseVersionNumberStr + "' into a number. ESD# string was '"+versionPart+"'. The version number will be set to " + Ver.ver(major, minor, maint, servicePack, patchLevel));
								}
							}
							if (subEsdStart != -1)
							{
								try
								{
									versionPart = aseEsdStr.trim().substring(subEsdStart, subEsdEnd);
									patchLevel = fixVersionOverflow(VERSION_PATCH_LEVEL, Integer.parseInt(versionPart));
//									int subEsdNum = fixVersionOverflow(VERSION_ROLLUP, Integer.parseInt(versionPart));
//									aseVersionNumber += 1 * subEsdNum;
								}
								catch (RuntimeException e) // NumberFormatException,
								{
									_logger.warn("Problems converting some part(s) of the ESD# in the version string '" + aseVersionNumberStr + "' into a number. ESD# string was '"+versionPart+"'. The version number will be set to " + Ver.ver(major, minor, maint, servicePack, patchLevel));
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
								_logger.warn("Problems converting some part(s) of the SP (ServicePack) in the version string '" + aseVersionNumberStr + "' into a number. Service Pack string was '"+versionPart+"'. The version number will be set to " + Ver.ver(major, minor, maint, servicePack, patchLevel));
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
//								aseVersionNumber += 1 * patchLevel;
							}
							catch (RuntimeException e) // NumberFormatException,
							{
								_logger.warn("Problems converting some part(s) of the SP (ServicePack) in the version string '" + aseVersionNumberStr + "' into a number. Service Pack string was '"+versionPart+"'. The version number will be set to " + Ver.ver(major, minor, maint, servicePack, patchLevel));
							}
						}
					}
				}
				// catch (NumberFormatException e)
				catch (RuntimeException e) // NumberFormatException,
											// IndexOutOfBoundsException
				{
					_logger.warn("Problems converting some part(s) of the version string '" + aseVersionNumberStr + "' into a number. The version number will be set to " + Ver.ver(major, minor, maint, servicePack, patchLevel));
				}
			}
			else
			{
				_logger.warn("There ASE version string seems to be faulty, can't find any '.' in the version number subsection '" + aseVersionNumberStr + "'.");
			}
		}
		else
		{
			_logger.warn("There ASE version string seems to be faulty, can't find any / in the string '" + versionStr + "'.");
		}

//System.out.println("  <- sybVersionStringToNumber(): <<<--- "+Ver.ver(major, minor, maint, servicePack, patchLevel));
		return Ver.ver(major, minor, maint, servicePack, patchLevel);
		//return aseVersionNumber;
	}











	//////////////////////////////////////////////
	// Some small TEST CODE
	//////////////////////////////////////////////
	public static void main(String[] args)
	{
		int version = 0;
		version = 125000300; System.out.println(version + " = "+ versionIntToStr(version));
		version = 125400100; System.out.println(version + " = "+ versionIntToStr(version));
		version = 125401002; System.out.println(version + " = "+ versionIntToStr(version));
		version = 157000420; System.out.println(version + " = "+ versionIntToStr(version));
		version = 157000000; System.out.println(version + " = "+ versionIntToStr(version));
		version = 157010000; System.out.println(version + " = "+ versionIntToStr(version));
		version = 157010200; System.out.println(version + " = "+ versionIntToStr(version));
		version = 157015000; System.out.println(version + " = "+ versionIntToStr(version));
		version = 157016000; System.out.println(version + " = "+ versionIntToStr(version));
		version = 157005000; System.out.println(version + " = "+ versionIntToStr(version));
		version = 157005100; System.out.println(version + " = "+ versionIntToStr(version));
		version = 157006000; System.out.println(version + " = "+ versionIntToStr(version));
		version = 160000000; System.out.println(version + " = "+ versionIntToStr(version));
		version = 160000001; System.out.println(version + " = "+ versionIntToStr(version));
		version = 160000100; System.out.println(version + " = "+ versionIntToStr(version));
		version = 160000101; System.out.println(version + " = "+ versionIntToStr(version));
		version = 161200101; System.out.println(version + " = "+ versionIntToStr(version));
		version = 999999999; System.out.println(version + " = "+ versionIntToStr(version));
		System.out.println("----------------------------------------------------------------------------------");

		version = Ver.ver(12,5,4, 10,  2); System.out.println(version + " = "+ versionIntToStr(version));
		version = Ver.ver(15,7,0, 100   ); System.out.println(version + " = "+ versionIntToStr(version));
		version = Ver.ver(15,7,0, 101   ); System.out.println(version + " = "+ versionIntToStr(version));
		version = Ver.ver(16,0          ); System.out.println(version + " = "+ versionIntToStr(version));
		version = Ver.ver(16,0,0, 0,   1); System.out.println(version + " = "+ versionIntToStr(version));
		version = Ver.ver(16,0,0, 1     ); System.out.println(version + " = "+ versionIntToStr(version));
		version = Ver.ver(16,0,0, 1,   1); System.out.println(version + " = "+ versionIntToStr(version));
		System.out.println("----------------------------------------------------------------------------------");

		version = Ver.ver(12,5,4, 10,  2); System.out.println(version + " = "+ versionIntToStr(version));
		version = Ver.ver(15,7,0, 51,  0); System.out.println(version + " = "+ versionIntToStr(version));
		version = Ver.ver(15,7,0, 101, 0); System.out.println(version + " = "+ versionIntToStr(version));
		version = Ver.ver(44,5,6, 777,88); System.out.println(version + " = "+ versionIntToStr(version));
//		version = Ver.ver(1, 2,3, 4,   5); System.out.println(version + " = "+ versionIntToStr(version)); // should fail... do to main<10
		System.out.println("----------------------------------------------------------------------------------");

		version = 1250030; System.out.println(version + " = "+ versionIntToStr(version));
		version = 1254010; System.out.println(version + " = "+ versionIntToStr(version));
		version = 1570042; System.out.println(version + " = "+ versionIntToStr(version));
		version = 1570000; System.out.println(version + " = "+ versionIntToStr(version));
		version = 1570100; System.out.println(version + " = "+ versionIntToStr(version));
		version = 1570120; System.out.println(version + " = "+ versionIntToStr(version));
		version = 1570150; System.out.println(version + " = "+ versionIntToStr(version));
		version = 1570160; System.out.println(version + " = "+ versionIntToStr(version));
		version = 1570050; System.out.println(version + " = "+ versionIntToStr(version));
		version = 1570051; System.out.println(version + " = "+ versionIntToStr(version));
		version = 1570060; System.out.println(version + " = "+ versionIntToStr(version));
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
		testVersion(Ver.ver(15,7,0,999), "Adaptive Server Enterprise/15.7/EBF XXXX SMP SP2000 /...");  
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
	}
	
	private static boolean testVersion(int expectedIntVer, String verStr)
	{
		int version = sybVersionStringToNumber(verStr);
		
		if (version != expectedIntVer) 
		{
			System.out.println("FAILED: version="+version+", expectedVersion="+expectedIntVer+", VersionStr='"+verStr+"'."); 
			return false;
		}
		else 
		{
			System.out.println("OK    : version="+version+", expectedVersion="+expectedIntVer+", VersionStr='"+verStr+"'."); 
			return true;
		}
	}
}
