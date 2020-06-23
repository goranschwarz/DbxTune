/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class VerTest
{

	@Test
	public void testSybaseAse()
	{
		assertEquals( 1_02_03_0004_0005L, Ver.ver( 1,2,3,  4,5));
		assertEquals(12_05_00_0003_0000L, Ver.ver(12,5,0,  3,0));
		assertEquals(12_05_04_0010_0002L, Ver.ver(12,5,4, 10,2));

		// 13+ digit numbers to string
		assertEquals("12.5 ESD#3",        Ver.versionNumToStr(12_05_00_0003_0000L) );
		assertEquals("12.5.4 ESD#1",      Ver.versionNumToStr(12_05_04_0001_0000L) );
		assertEquals("12.5.4 ESD#10.2",   Ver.versionNumToStr(12_05_04_0010_0002L) );
		assertEquals("15.7 ESD#4.20",     Ver.versionNumToStr(15_07_00_0004_0020L) );
		assertEquals("15.7",              Ver.versionNumToStr(15_07_00_0000_0000L) );
		assertEquals("15.7 SP100",        Ver.versionNumToStr(15_07_00_0100_0000L) );
		assertEquals("15.7 SP102",        Ver.versionNumToStr(15_07_00_0102_0000L) );
		assertEquals("15.7 SP150",        Ver.versionNumToStr(15_07_00_0150_0000L) );
		assertEquals("15.7 SP160",        Ver.versionNumToStr(15_07_00_0160_0000L) );
		assertEquals("15.7 SP50",         Ver.versionNumToStr(15_07_00_0050_0000L) );
		assertEquals("15.7 SP51",         Ver.versionNumToStr(15_07_00_0051_0000L) );
		assertEquals("15.7 SP60",         Ver.versionNumToStr(15_07_00_0060_0000L) );
		assertEquals("16.0",              Ver.versionNumToStr(16_00_00_0000_0000L) );
		assertEquals("16.0 PL01",         Ver.versionNumToStr(16_00_00_0000_0001L) );
		assertEquals("16.0 SP01",         Ver.versionNumToStr(16_00_00_0001_0000L) );
		assertEquals("16.0 SP01 PL01",    Ver.versionNumToStr(16_00_00_0001_0001L) );
		assertEquals("16.1.2 SP01 PL01",  Ver.versionNumToStr(16_01_02_0001_0001L) );
		assertEquals("99.99.99 SP9999 PL9999", Ver.versionNumToStr(99_99_99_9999_9999L) );


		// 9 digit numbers to string
		assertEquals("12.5 ESD#3",        Ver.versionNumToStr(1250_003_00) );
		assertEquals("12.5.4 ESD#1",      Ver.versionNumToStr(1254_001_00) );
		assertEquals("12.5.4 ESD#10.2",   Ver.versionNumToStr(1254_010_02) );
		assertEquals("15.7 ESD#4.2",      Ver.versionNumToStr(1570_004_02) );
		assertEquals("15.7 ESD#4.20",     Ver.versionNumToStr(1570_004_20) );
		assertEquals("15.7",              Ver.versionNumToStr(1570_000_00) );
		assertEquals("15.7 SP100",        Ver.versionNumToStr(1570_100_00) );
		assertEquals("15.7 SP102",        Ver.versionNumToStr(1570_102_00) );
		assertEquals("15.7 SP150",        Ver.versionNumToStr(1570_150_00) );
		assertEquals("15.7 SP160",        Ver.versionNumToStr(1570_160_00) );
		assertEquals("15.7 SP50",         Ver.versionNumToStr(1570_050_00) );
		assertEquals("15.7 SP51",         Ver.versionNumToStr(1570_051_00) );
		assertEquals("15.7 SP60",         Ver.versionNumToStr(1570_060_00) );
		assertEquals("16.0",              Ver.versionNumToStr(1600_000_00) );
		assertEquals("16.0 PL01",         Ver.versionNumToStr(1600_000_01) );
		assertEquals("16.0 SP01",         Ver.versionNumToStr(1600_001_00) );
		assertEquals("16.0 SP01 PL01",    Ver.versionNumToStr(1600_001_01) );
		assertEquals("16.1.2 SP01 PL01",  Ver.versionNumToStr(1612_001_01) );
		assertEquals("99.9.9 SP999 PL99", Ver.versionNumToStr(9999_999_99) );


		// Ver.ver() + Ver.versionNumToStr()
		assertEquals("12.5.4 ESD#10.2",   Ver.versionNumToStr(Ver.ver(12,5,4, 10,  2)) );
		assertEquals("15.7 SP100",        Ver.versionNumToStr(Ver.ver(15,7,0, 100   )) );
		assertEquals("15.7 SP101",        Ver.versionNumToStr(Ver.ver(15,7,0, 101   )) );
		assertEquals("16.0",              Ver.versionNumToStr(Ver.ver(16,0          )) );
		assertEquals("16.0 PL01",         Ver.versionNumToStr(Ver.ver(16,0,0, 0,   1)) );
		assertEquals("16.0 SP01",         Ver.versionNumToStr(Ver.ver(16,0,0, 1     )) );
		assertEquals("16.0 SP01 PL01",    Ver.versionNumToStr(Ver.ver(16,0,0, 1,   1)) );

		assertEquals("12.5.4 ESD#10.2",   Ver.versionNumToStr(Ver.ver(12,5,4, 10,  2)) );
		assertEquals("15.7 SP51",         Ver.versionNumToStr(Ver.ver(15,7,0, 51,  0)) );
		assertEquals("15.7 SP101",        Ver.versionNumToStr(Ver.ver(15,7,0, 101, 0)) );
		assertEquals("44.5.6 SP777 PL88", Ver.versionNumToStr(Ver.ver(44,5,6, 777,88)) );
//		version = Ver.ver(1, 2,3, 4,   5); System.out.println(version + " = "+ Ver.versionNumToStr(version)); // should fail... do to main<10

		
		// 7 digit int
//		assertEquals("12.5.0 ESD#3"   , Ver.versionNumToStr(1250_030));
//		assertEquals("12.5.4 ESD#1"   , Ver.versionNumToStr(1254_010));
//		assertEquals("15.7.0 ESD#4.2" , Ver.versionNumToStr(1570_042));
		assertEquals("15.7"           , Ver.versionNumToStr(1570_000));
		assertEquals("15.7 SP100"     , Ver.versionNumToStr(1570_100));
		assertEquals("15.7 SP120"     , Ver.versionNumToStr(1570_120));
		assertEquals("15.7 SP150"     , Ver.versionNumToStr(1570_150));
		assertEquals("15.7 SP160"     , Ver.versionNumToStr(1570_160));
		assertEquals("15.7 SP50"      , Ver.versionNumToStr(1570_050));
		assertEquals("15.7 SP51"      , Ver.versionNumToStr(1570_051));
		assertEquals("15.7 SP60"      , Ver.versionNumToStr(1570_060));



//		assertEquals(1250011, "12.5.0 ESD#1.1");
//		assertEquals(1250030, "Adaptive Server Enterprise/12.5.0.3/EBF 11449 ESD#4/...");
////		assertEquals(1250090, "Adaptive Server Enterprise/12.5.0.10/P/x86_64/...");
//		assertEquals(1254010, "Adaptive Server Enterprise/12.5.4/EBF 16748 SMP ESD#1 /P/x86_64/...");
//		assertEquals(1254090, "Adaptive Server Enterprise/12.5.4/EBF 16748 SMP ESD#11/P/x86_64/...");
//		
//		assertEquals(1550015, "15.5.0 ESD#1.5");
//		assertEquals(1502050, "Adaptive Server Enterprise/15.0.2/EBF 16748 SMP ESD#5 /P/x86_64/...");
//		assertEquals(1503040, "Adaptive Server Enterprise/15.0.3/EBF 16748 SMP ESD#4 /P/x86_64/...");
//		assertEquals(1503042, "Adaptive Server Enterprise/15.0.3/EBF 16748 SMP ESD#4.2 /P/x86_64/...");
//		
//		assertEquals(1570040, "Adaptive Server Enterprise/15.7.0/EBF 16748 SMP ESD#4 /P/x86_64/...");
//		assertEquals(1570042, "Adaptive Server Enterprise/15.7.0/EBF 16748 SMP ESD#4.2 /P/x86_64/...");
//
//		assertEquals(1570100, "Adaptive Server Enterprise/15.7/EBF 16748 SMP SP100 /P/x86_64/...");   
//		assertEquals(1570101, "Adaptive Server Enterprise/15.7/EBF 16748 SMP SP101 /P/x86_64/...");   
//		assertEquals(1570111, "Adaptive Server Enterprise/15.7/EBF 16748 SMP SP111 /P/x86_64/...");   
//		assertEquals(1570200, "Adaptive Server Enterprise/15.7/EBF 16748 SMP SP200 /P/x86_64/...");   
//		assertEquals(1570999, "Adaptive Server Enterprise/15.7/EBF 16748 SMP SP2000 /P/x86_64/...");  
//		assertEquals(1571100, "Adaptive Server Enterprise/15.7.1/EBF 16748 SMP SP100 /P/x86_64/...");   
//
//		assertEquals(1570050, "Adaptive Server Enterprise/15.7.0/EBF 21207 SMP SP50 /P/Solaris AMD64/...");
//		assertEquals(1570051, "Adaptive Server Enterprise/15.7.0/EBF 21757 SMP SP51 /P/x86_64/Enterprise Linux/...");

		assertEquals(Ver.ver(12,5,0,1,1), Ver.sybVersionStringToNumber("12.5.0 ESD#1.1"));
		assertEquals(Ver.ver(12,5,0,3),   Ver.sybVersionStringToNumber("Adaptive Server Enterprise/12.5.0.3/EBF XXXX ESD#4/..."));
		assertEquals(Ver.ver(12,5,0,10),  Ver.sybVersionStringToNumber("Adaptive Server Enterprise/12.5.0.10/..."));
		assertEquals(Ver.ver(12,5,4,1),   Ver.sybVersionStringToNumber("Adaptive Server Enterprise/12.5.4/EBF XXXX SMP ESD#1 /..."));
		assertEquals(Ver.ver(12,5,4,11),  Ver.sybVersionStringToNumber("Adaptive Server Enterprise/12.5.4/EBF XXXX SMP ESD#11/..."));
		
		assertEquals(Ver.ver(15,5,0,1,5), Ver.sybVersionStringToNumber("15.5.0 ESD#1.5"));
		assertEquals(Ver.ver(15,0,2,5),   Ver.sybVersionStringToNumber("Adaptive Server Enterprise/15.0.2/EBF XXXX SMP ESD#5 /..."));
		assertEquals(Ver.ver(15,0,3,4),   Ver.sybVersionStringToNumber("Adaptive Server Enterprise/15.0.3/EBF XXXX SMP ESD#4 /..."));
		assertEquals(Ver.ver(15,0,3,4,2), Ver.sybVersionStringToNumber("Adaptive Server Enterprise/15.0.3/EBF XXXX SMP ESD#4.2 /..."));
		
		assertEquals(Ver.ver(15,7,0,4),   Ver.sybVersionStringToNumber("Adaptive Server Enterprise/15.7.0/EBF XXXX SMP ESD#4 /..."));
		assertEquals(Ver.ver(15,7,0,4,2), Ver.sybVersionStringToNumber("Adaptive Server Enterprise/15.7.0/EBF XXXX SMP ESD#4.2 /..."));

		assertEquals(Ver.ver(15,7,0,100), Ver.sybVersionStringToNumber("Adaptive Server Enterprise/15.7/EBF XXXX SMP SP100 /..."));   
		assertEquals(Ver.ver(15,7,0,101), Ver.sybVersionStringToNumber("Adaptive Server Enterprise/15.7/EBF XXXX SMP SP101 /..."));   
		assertEquals(Ver.ver(15,7,0,111), Ver.sybVersionStringToNumber("Adaptive Server Enterprise/15.7/EBF XXXX SMP SP111 /..."));   
		assertEquals(Ver.ver(15,7,0,200), Ver.sybVersionStringToNumber("Adaptive Server Enterprise/15.7/EBF XXXX SMP SP200 /..."));   
		assertEquals(Ver.ver(15,7,0,2000),Ver.sybVersionStringToNumber("Adaptive Server Enterprise/15.7/EBF XXXX SMP SP2000 /..."));  
		assertEquals(Ver.ver(15,7,1,100), Ver.sybVersionStringToNumber("Adaptive Server Enterprise/15.7.1/EBF XXXX SMP SP100 /..."));   

		assertEquals(Ver.ver(15,7,0,50),  Ver.sybVersionStringToNumber("Adaptive Server Enterprise/15.7.0/EBF XXXX SMP SP50 /..."));
		assertEquals(Ver.ver(15,7,0,51),  Ver.sybVersionStringToNumber("Adaptive Server Enterprise/15.7.0/EBF XXXX SMP SP51 /..."));

		assertEquals(Ver.ver(16,0),       Ver.sybVersionStringToNumber("Adaptive Server Enterprise/16.0/EBF XXXX SMP /..."));
		assertEquals(Ver.ver(16,0,0,0,1), Ver.sybVersionStringToNumber("Adaptive Server Enterprise/16.0/EBF XXXX SMP PL1 /..."));
		assertEquals(Ver.ver(16,0,0,0,1), Ver.sybVersionStringToNumber("Adaptive Server Enterprise/16.0/EBF XXXX SMP PL01 /..."));
		assertEquals(Ver.ver(16,0,0,1),   Ver.sybVersionStringToNumber("Adaptive Server Enterprise/16.0/EBF XXXX SMP SP1 /..."));
		assertEquals(Ver.ver(16,0,0,1),   Ver.sybVersionStringToNumber("Adaptive Server Enterprise/16.0/EBF XXXX SMP SP01 /..."));
		assertEquals(Ver.ver(16,0,0,1,1), Ver.sybVersionStringToNumber("Adaptive Server Enterprise/16.0/EBF XXXX SMP SP1 PL1/..."));
		assertEquals(Ver.ver(16,0,0,1,1), Ver.sybVersionStringToNumber("Adaptive Server Enterprise/16.0/EBF XXXX SMP SP01 PL01/..."));
		assertEquals(Ver.ver(16,0,1,1,1), Ver.sybVersionStringToNumber("Adaptive Server Enterprise/16.0.1/EBF XXXX SMP SP1 PL1/..."));
		assertEquals(Ver.ver(16,0,1,1,1), Ver.sybVersionStringToNumber("Adaptive Server Enterprise/16.0.1/EBF XXXX SMP SP01 PL01/..."));
		assertEquals(Ver.ver(16,1,0,1,1), Ver.sybVersionStringToNumber("Adaptive Server Enterprise/16.1/EBF XXXX SMP SP01 PL01/..."));

		// Check version with another format
		assertEquals(Ver.ver(16,0),       Ver.sybVersionStringToNumber("Adaptive Server Enterprise/16.0/EBF XXXX SMP /..."));
		assertEquals(Ver.ver(16,0,0,0,1), Ver.sybVersionStringToNumber("Adaptive Server Enterprise/16.0 PL1/EBF XXXX SMP /..."));
		assertEquals(Ver.ver(16,0,0,0,1), Ver.sybVersionStringToNumber("Adaptive Server Enterprise/16.0 PL01 /EBF XXXX SMP /..."));
		assertEquals(Ver.ver(16,0,0,1),   Ver.sybVersionStringToNumber("Adaptive Server Enterprise/16.0 SP1 /EBF XXXX SMP /..."));
		assertEquals(Ver.ver(16,0,0,1),   Ver.sybVersionStringToNumber("Adaptive Server Enterprise/16.0 SP01/EBF XXXX SMP /..."));
		assertEquals(Ver.ver(16,0,0,1,1), Ver.sybVersionStringToNumber("Adaptive Server Enterprise/16.0 SP1 PL1/EBF XXXX SMP/..."));
		assertEquals(Ver.ver(16,0,0,1,1), Ver.sybVersionStringToNumber("Adaptive Server Enterprise/16.0 SP01 PL01/EBF XXXX SMP /..."));
		assertEquals(Ver.ver(16,0,1,1,1), Ver.sybVersionStringToNumber("Adaptive Server Enterprise/16.0.1 SP1 PL1/EBF XXXX SMP /..."));
		assertEquals(Ver.ver(16,0,1,1,1), Ver.sybVersionStringToNumber("Adaptive Server Enterprise/16.0.1 SP01 PL01/EBF XXXX SMP /..."));
		assertEquals(Ver.ver(16,1,0,1,1), Ver.sybVersionStringToNumber("Adaptive Server Enterprise/16.1 SP01 PL01/EBF XXXX SMP /..."));

		// Some real life version strings for 16
		assertEquals(Ver.ver(16,0,0,0,0), Ver.sybVersionStringToNumber("Adaptive Server Enterprise/16.0/EBF 22385 SMP/P/X64/Windows Server/asecepheus/3530/64-bit/FBO/Sun Feb 16 06:52:50 2014"));
		assertEquals(Ver.ver(16,0,0,0,1), Ver.sybVersionStringToNumber("Adaptive Server Enterprise/16.0 GA PL01/EBF 22544 SMP/P/x86_64/Enterprise Linux/ase160sp00pl01/3523/64-bit/FBO/Tue Apr 15 13:24:31 2014"));
	}
		
		
//	@Test
//	public void testSybaseIq()
//	{
//		// Some IQ real life
//		assertEquals(Ver.ver(16,0,0, 4, 6), "Sybase IQ/16.0.0.656/140812/P/sp04.06/RS6000MP/AIX 6.1.0/64bit/2014-08-12 12:26:08");
//		assertEquals(Ver.ver(15,4,0, 5, 0), "Sybase IQ/15.4.0.3046/141204/P/ESD 5/Enterprise Linux64 - x86_64 - 2.6.18-194.el5/64bit/2014-12-04 17:24:15");
//		assertEquals(Ver.ver(15,4,0, 2, 0), "Sybase IQ/15.4.0.3019/120816/P/ESD 2/Sun_Sparc/OS 5.10/64bit/2012-08-16 12:40:47");
//		assertEquals(Ver.ver(15,2,0, 2, 0), "Sybase IQ/15.2.0.5615/101123/P/ESD 2/Enterprise Linux64 - x86_64 - 2.6.9-67.0.4.ELsmp/64bit/2010-11-23 10:53:30");
//		assertEquals(Ver.ver(16,0,0, 10,6), "SAP IQ/16.0.102.2.1297/20080/P/sp10.06/MS/Windows 2003/64bit/2015-10-27 01:04:33");
//		assertEquals(Ver.ver(16,0,0, 10,3), "SAP IQ/16.0.102.1257/20056/P/sp10.03/Enterprise Linux64 - x86_64 - 2.6.18-194.el5/64bit/2015-08-20 17:45:23");
//		assertEquals(Ver.ver(16,0,0, 8,38), "SAP IQ/16.0.0.809/150928/P/sp08.38/RS6000MP/AIX 6.1.0/64bit/2015-09-28 09:22:15");
//		assertEquals(Ver.ver(16,0,0, 8,27), "SAP IQ/16.0.0.808/150223/P/sp08.27/MS/Windows 2003/64bit/2015-02-23 16:42:29");
//
//		// Some IQ Made up in case of SAP decides to go: SP PL
//		assertEquals(Ver.ver(16,1,0, 1, 0), "SAP IQ/16.1.102.2.1297/20080/P/SP01/MS/Windows 2003/64bit/2015-10-27 01:04:33");
//		assertEquals(Ver.ver(16,1,0, 20,1), "SAP IQ/16.1.102.2.1297/20080/P/SP20 PL01/MS/Windows 2003/64bit/2015-10-27 01:04:33");
//		assertEquals(Ver.ver(16,1,0, 1, 0), "SAP IQ/16.1.102.2.1297/20080/P/xxx SP01/MS/Windows 2003/64bit/2015-10-27 01:04:33");
//		assertEquals(Ver.ver(16,1,0, 20,1), "SAP IQ/16.1.102.2.1297/20080/P/xxx SP20 PL01/MS/Windows 2003/64bit/2015-10-27 01:04:33");
//
//		// Some IQ Made up in case of SAP decides to go: SP PL and also removes everything after 16.1  16.1<.102.2.1297> 
//		assertEquals(Ver.ver(16,1,0, 1, 0), "SAP IQ/16.1/20080/P/SP01/MS/...");
//		assertEquals(Ver.ver(16,1,0, 20,1), "SAP IQ/16.1/20080/P/SP20 PL01/MS/...");
//		assertEquals(Ver.ver(16,1,0, 1, 0), "SAP IQ/16.1/20080/P/xxx SP01/MS/...");
//		assertEquals(Ver.ver(16,1,0, 20,1), "SAP IQ/16.1/20080/P/xxx SP20 PL01/MS/...");
//		// Some IQ Made up in case of SAP decides to go: 16.1 SP## PL## like it's in ASE
//		assertEquals(Ver.ver(16,1,0, 1, 0), "SAP IQ/16.1 SP01/20080/P/MS/...");
//		assertEquals(Ver.ver(16,1,0, 0, 1), "SAP IQ/16.1 PL01/20080/P/MS/...");
//		assertEquals(Ver.ver(16,1,0, 20,1), "SAP IQ/16.1 SP20 PL01/20080/P/MS/...");
//	}

	@Test
	public void testSqlServer()
	{
		// Some real life
		assertEquals(Ver.ver(2017,0,0, 0, 9), Ver.sqlServerVersionStringToNumber("Microsoft SQL Server 2017 (RTM-CU9) (KB4341265) - 14.0.3030.27 (X64) Jun 29 2018 18:02:47 "));
		assertEquals(Ver.ver(2017,0,0, 0, 8), Ver.sqlServerVersionStringToNumber("Microsoft SQL Server 2017 (RTM-CU8) (KB4338363) - 14.0.3029.16 (X64) Jun 13 2018 13:35:56 "));
		assertEquals(Ver.ver(2017,0,0, 0, 3), Ver.sqlServerVersionStringToNumber("Microsoft SQL Server 2017 (RTM-CU3-GDR) (KB4052987) - 14.0.3015.40 (X64) Dec 22 2017 16:13"));
		assertEquals(Ver.ver(2017,0,0, 0, 3), Ver.sqlServerVersionStringToNumber("Microsoft SQL Server 2017 (RTM-CU3-GDR) (KB4052987) - 14.0.3015.40 (X64) Dec 22 2017 16:13"));
		assertEquals(Ver.ver(2016,0,0, 2, 3), Ver.sqlServerVersionStringToNumber("Microsoft SQL Server 2016 (SP2-CU3) (KB4458871) - 13.0.5216.0 (X64) Sep 13 2018 22:16:01 C"));
		assertEquals(Ver.ver(2016,0,0, 1, 3), Ver.sqlServerVersionStringToNumber("Microsoft SQL Server 2016 (SP1-CU3) (KB4019916) - 13.0.4435.0 (X64) Apr 27 2017 17:36:12 C"));
		assertEquals(Ver.ver(2016,0,0, 1, 0), Ver.sqlServerVersionStringToNumber("Microsoft SQL Server 2016 (SP1) (KB3182545) - 13.0.4001.0 (X64) Oct 28 2016 18:17:30 Copyr"));
		assertEquals(Ver.ver(2016,0,0, 1, 0), Ver.sqlServerVersionStringToNumber("Microsoft SQL Server 2016 (SP1) (KB3182545) - 13.0.4001.0 (X64) Oct 28 2016 18:17:30 Copyr"));
		assertEquals(Ver.ver(2016,0,0, 1, 0), Ver.sqlServerVersionStringToNumber("Microsoft SQL Server 2016 (SP1) (KB3182545) - 13.0.4001.0 (X64) Oct 28 2016 18:17:30 Copyr"));
		assertEquals(Ver.ver(2016,0,0, 1, 0), Ver.sqlServerVersionStringToNumber("Microsoft SQL Server 2016 (SP1) (KB3182545) - 13.0.4001.0 (X64) Oct 28 2016 18:17:30 Copyr"));
		assertEquals(Ver.ver(2014,0,0, 0, 0), Ver.sqlServerVersionStringToNumber("Microsoft SQL Server 2014 - 12.0.4213.0 (X64) Jun 9 2015 12:06:16 Copyright (c) Microsoft "));
		assertEquals(Ver.ver(2014,0,0, 0, 0), Ver.sqlServerVersionStringToNumber("Microsoft SQL Server 2014 - 12.0.4213.0 (X64) Jun 9 2015 12:06:16 Copyright (c) Microsoft "));
		assertEquals(Ver.ver(2014,0,0, 2, 0), Ver.sqlServerVersionStringToNumber("Microsoft SQL Server 2014 (SP2-GDR) (KB4019093) - 12.0.5207.0 (X64) Jul 3 2017 02:25:44 Co"));
		assertEquals(Ver.ver(2014,0,0, 2, 5), Ver.sqlServerVersionStringToNumber("Microsoft SQL Server 2014 (SP2-CU5) (KB4013098) - 12.0.5546.0 (X64) Apr 3 2017 14:55:37 Co"));
		assertEquals(Ver.ver(2014,0,0, 2, 5), Ver.sqlServerVersionStringToNumber("Microsoft SQL Server 2014 (SP2-CU5) (KB4013098) - 12.0.5546.0 (X64) Apr 3 2017 14:55:37 Co"));
		assertEquals(Ver.ver(2014,0,0, 2,10), Ver.sqlServerVersionStringToNumber("Microsoft SQL Server 2014 (SP2-CU10-GDR) (KB4052725) - 12.0.5571.0 (X64) Jan 10 2018 15:52"));
		assertEquals(Ver.ver(2014,0,0, 2, 0), Ver.sqlServerVersionStringToNumber("Microsoft SQL Server 2014 (SP2) (KB3171021) - 12.0.5000.0 (X64) Jun 17 2016 19:14:09 Copyr"));
		assertEquals(Ver.ver(2014,0,0, 1, 4), Ver.sqlServerVersionStringToNumber("Microsoft SQL Server 2014 (SP1-CU4) (KB3106660) - 12.0.4436.0 (X64) Dec 2 2015 16:09:44 Co"));
		assertEquals(Ver.ver(2014,0,0, 1,13), Ver.sqlServerVersionStringToNumber("Microsoft SQL Server 2014 (SP1-CU13) (KB4019099) - 12.0.4522.0 (X64) Jun 28 2017 17:36:31 "));
		assertEquals(Ver.ver(2012,0,0, 0, 0), Ver.sqlServerVersionStringToNumber("Microsoft SQL Server 2012 - 11.0.5592.0 (X64) Apr 17 2015 15:18:46 Copyright (c) Microsoft"));
		assertEquals(Ver.ver(2012,0,0, 0, 0), Ver.sqlServerVersionStringToNumber("Microsoft SQL Server 2012 - 11.0.5058.0 (X64) May 14 2014 18:34:29 Copyright (c) Microsoft"));
		assertEquals(Ver.ver(2012,0,0, 0, 0), Ver.sqlServerVersionStringToNumber("Microsoft SQL Server 2012 - 11.0.5058.0 (X64) May 14 2014 18:34:29 Copyright (c) Microsoft"));
		assertEquals(Ver.ver(2012,0,0, 4, 0), Ver.sqlServerVersionStringToNumber("Microsoft SQL Server 2012 (SP4) (KB4018073) - 11.0.7001.0 (X64) Aug 15 2017 10:23:29 Copyr"));
		assertEquals(Ver.ver(2012,0,0, 3, 0), Ver.sqlServerVersionStringToNumber("Microsoft SQL Server 2012 (SP3-GDR) (KB3194721) - 11.0.6248.0 (X64) Sep 23 2016 15:49:43 C"));
		assertEquals(Ver.ver(2012,0,0, 3, 2), Ver.sqlServerVersionStringToNumber("Microsoft SQL Server 2012 (SP3-CU2) (KB3137746) - 11.0.6523.0 (X64) Mar 2 2016 21:29:16 Co"));
		assertEquals(Ver.ver(2012,0,0, 3,10), Ver.sqlServerVersionStringToNumber("Microsoft SQL Server 2012 (SP3-CU10) (KB4025925) - 11.0.6607.3 (X64) Jul 8 2017 16:43:40 C"));
		assertEquals(Ver.ver(2012,0,0, 3, 0), Ver.sqlServerVersionStringToNumber("Microsoft SQL Server 2012 (SP3) (KB3072779) - 11.0.6020.0 (X64) Oct 20 2015 15:36:27 Copyr"));
		assertEquals(Ver.ver(2012,0,0, 1, 0), Ver.sqlServerVersionStringToNumber("Microsoft SQL Server 2012 (SP1) - 11.0.3381.0 (X64) Aug 23 2013 20:08:13 Copyright (c) Mic"));
		assertEquals(Ver.ver(2008,2,0, 3, 0), Ver.sqlServerVersionStringToNumber("Microsoft SQL Server 2008 R2 (SP3) - 10.50.6220.0 (X64) Mar 19 2015 12:32:14 Copyright (c)"));
		assertEquals(Ver.ver(2008,2,0, 3, 0), Ver.sqlServerVersionStringToNumber("Microsoft SQL Server 2008 R2 (SP3) - 10.50.6000.34 (X64) Aug 19 2014 12:21:34 Copyright (c"));
		assertEquals(Ver.ver(2008,2,0, 3, 0), Ver.sqlServerVersionStringToNumber("Microsoft SQL Server 2008 R2 (SP3) - 10.50.6000.34 (X64) Aug 19 2014 12:21:34 Copyright (c"));
		assertEquals(Ver.ver(2008,2,0, 1, 0), Ver.sqlServerVersionStringToNumber("Microsoft SQL Server 2008 R2 (SP1) - 10.50.2500.0 (X64) Jun 17 2011 00:54:03 Copyright (c)"));
		assertEquals(Ver.ver(2008,2,0, 0, 0), Ver.sqlServerVersionStringToNumber("Microsoft SQL Server 2008 R2 (RTM) - 10.50.1600.1 (X64) Apr 2 2010 15:48:46 Copyright (c) "));
		assertEquals(Ver.ver(2008,0,0, 1, 0), Ver.sqlServerVersionStringToNumber("Microsoft SQL Server 2008 (SP1) - 10.0.2714.0 (X64) May 14 2009 16:08:52 Copyright (c) 198"));		
	}
}
