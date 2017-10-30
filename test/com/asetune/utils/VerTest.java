package com.asetune.utils;

import static org.junit.Assert.*;

import org.junit.Test;

public class VerTest
{

	@Test
	public void testSybaseAse()
	{
		// 9 digit numbers to string
		assertEquals("12.5 ESD#3",        Ver.versionIntToStr(125000300) );
		assertEquals("12.5.4 ESD#1",      Ver.versionIntToStr(125400100) );
		assertEquals("12.5.4 ESD#10.2",   Ver.versionIntToStr(125401002) );
		assertEquals("15.7 ESD#4.20",     Ver.versionIntToStr(157000420) );
		assertEquals("15.7",              Ver.versionIntToStr(157000000) );
		assertEquals("15.7 SP100",        Ver.versionIntToStr(157010000) );
		assertEquals("15.7 SP102",        Ver.versionIntToStr(157010200) );
		assertEquals("15.7 SP150",        Ver.versionIntToStr(157015000) );
		assertEquals("15.7 SP160",        Ver.versionIntToStr(157016000) );
		assertEquals("15.7 SP50",         Ver.versionIntToStr(157005000) );
		assertEquals("15.7 SP51",         Ver.versionIntToStr(157005100) );
		assertEquals("15.7 SP60",         Ver.versionIntToStr(157006000) );
		assertEquals("16.0",              Ver.versionIntToStr(160000000) );
		assertEquals("16.0 PL01",         Ver.versionIntToStr(160000001) );
		assertEquals("16.0 SP01",         Ver.versionIntToStr(160000100) );
		assertEquals("16.0 SP01 PL01",    Ver.versionIntToStr(160000101) );
		assertEquals("16.1.2 SP01 PL01",  Ver.versionIntToStr(161200101) );
		assertEquals("99.9.9 SP999 PL99", Ver.versionIntToStr(999999999) );


		// Ver.ver() + Ver.versionIntToStr()
		assertEquals("12.5.4 ESD#10.2",   Ver.versionIntToStr(Ver.ver(12,5,4, 10,  2)) );
		assertEquals("15.7 SP100",        Ver.versionIntToStr(Ver.ver(15,7,0, 100   )) );
		assertEquals("15.7 SP101",        Ver.versionIntToStr(Ver.ver(15,7,0, 101   )) );
		assertEquals("16.0",              Ver.versionIntToStr(Ver.ver(16,0          )) );
		assertEquals("16.0 PL01",         Ver.versionIntToStr(Ver.ver(16,0,0, 0,   1)) );
		assertEquals("16.0 SP01",         Ver.versionIntToStr(Ver.ver(16,0,0, 1     )) );
		assertEquals("16.0 SP01 PL01",    Ver.versionIntToStr(Ver.ver(16,0,0, 1,   1)) );

		assertEquals("12.5.4 ESD#10.2",   Ver.versionIntToStr(Ver.ver(12,5,4, 10,  2)) );
		assertEquals("15.7 SP51",         Ver.versionIntToStr(Ver.ver(15,7,0, 51,  0)) );
		assertEquals("15.7 SP101",        Ver.versionIntToStr(Ver.ver(15,7,0, 101, 0)) );
		assertEquals("44.5.6 SP777 PL88", Ver.versionIntToStr(Ver.ver(44,5,6, 777,88)) );
//		version = Ver.ver(1, 2,3, 4,   5); System.out.println(version + " = "+ Ver.versionIntToStr(version)); // should fail... do to main<10

		
		// 7 digit int
		assertEquals("12.5.0 ESD#3"   , Ver.versionIntToStr(1250030));
		assertEquals("12.5.4 ESD#1"   , Ver.versionIntToStr(1254010));
		assertEquals("15.7.0 ESD#4.2" , Ver.versionIntToStr(1570042));
		assertEquals("15.7"           , Ver.versionIntToStr(1570000));
		assertEquals("15.7 SP100"     , Ver.versionIntToStr(1570100));
		assertEquals("15.7 SP120"     , Ver.versionIntToStr(1570120));
		assertEquals("15.7 SP150"     , Ver.versionIntToStr(1570150));
		assertEquals("15.7 SP160"     , Ver.versionIntToStr(1570160));
		assertEquals("15.7.0 SP50"    , Ver.versionIntToStr(1570050));
		assertEquals("15.7.0 SP51"    , Ver.versionIntToStr(1570051));
		assertEquals("15.7.0 SP60"    , Ver.versionIntToStr(1570060));



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
		assertEquals(Ver.ver(15,7,0,999), Ver.sybVersionStringToNumber("Adaptive Server Enterprise/15.7/EBF XXXX SMP SP2000 /..."));  
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
		
		
	public void testSybaseIq()
	{
		// Some IQ real life
		assertEquals(Ver.ver(16,0,0, 4, 6), "Sybase IQ/16.0.0.656/140812/P/sp04.06/RS6000MP/AIX 6.1.0/64bit/2014-08-12 12:26:08");
		assertEquals(Ver.ver(15,4,0, 5, 0), "Sybase IQ/15.4.0.3046/141204/P/ESD 5/Enterprise Linux64 - x86_64 - 2.6.18-194.el5/64bit/2014-12-04 17:24:15");
		assertEquals(Ver.ver(15,4,0, 2, 0), "Sybase IQ/15.4.0.3019/120816/P/ESD 2/Sun_Sparc/OS 5.10/64bit/2012-08-16 12:40:47");
		assertEquals(Ver.ver(15,2,0, 2, 0), "Sybase IQ/15.2.0.5615/101123/P/ESD 2/Enterprise Linux64 - x86_64 - 2.6.9-67.0.4.ELsmp/64bit/2010-11-23 10:53:30");
		assertEquals(Ver.ver(16,0,0, 10,6), "SAP IQ/16.0.102.2.1297/20080/P/sp10.06/MS/Windows 2003/64bit/2015-10-27 01:04:33");
		assertEquals(Ver.ver(16,0,0, 10,3), "SAP IQ/16.0.102.1257/20056/P/sp10.03/Enterprise Linux64 - x86_64 - 2.6.18-194.el5/64bit/2015-08-20 17:45:23");
		assertEquals(Ver.ver(16,0,0, 8,38), "SAP IQ/16.0.0.809/150928/P/sp08.38/RS6000MP/AIX 6.1.0/64bit/2015-09-28 09:22:15");
		assertEquals(Ver.ver(16,0,0, 8,27), "SAP IQ/16.0.0.808/150223/P/sp08.27/MS/Windows 2003/64bit/2015-02-23 16:42:29");

		// Some IQ Made up in case of SAP decides to go: SP PL
		assertEquals(Ver.ver(16,1,0, 1, 0), "SAP IQ/16.1.102.2.1297/20080/P/SP01/MS/Windows 2003/64bit/2015-10-27 01:04:33");
		assertEquals(Ver.ver(16,1,0, 20,1), "SAP IQ/16.1.102.2.1297/20080/P/SP20 PL01/MS/Windows 2003/64bit/2015-10-27 01:04:33");
		assertEquals(Ver.ver(16,1,0, 1, 0), "SAP IQ/16.1.102.2.1297/20080/P/xxx SP01/MS/Windows 2003/64bit/2015-10-27 01:04:33");
		assertEquals(Ver.ver(16,1,0, 20,1), "SAP IQ/16.1.102.2.1297/20080/P/xxx SP20 PL01/MS/Windows 2003/64bit/2015-10-27 01:04:33");

		// Some IQ Made up in case of SAP decides to go: SP PL and also removes everything after 16.1  16.1<.102.2.1297> 
		assertEquals(Ver.ver(16,1,0, 1, 0), "SAP IQ/16.1/20080/P/SP01/MS/...");
		assertEquals(Ver.ver(16,1,0, 20,1), "SAP IQ/16.1/20080/P/SP20 PL01/MS/...");
		assertEquals(Ver.ver(16,1,0, 1, 0), "SAP IQ/16.1/20080/P/xxx SP01/MS/...");
		assertEquals(Ver.ver(16,1,0, 20,1), "SAP IQ/16.1/20080/P/xxx SP20 PL01/MS/...");
		// Some IQ Made up in case of SAP decides to go: 16.1 SP## PL## like it's in ASE
		assertEquals(Ver.ver(16,1,0, 1, 0), "SAP IQ/16.1 SP01/20080/P/MS/...");
		assertEquals(Ver.ver(16,1,0, 0, 1), "SAP IQ/16.1 PL01/20080/P/MS/...");
		assertEquals(Ver.ver(16,1,0, 20,1), "SAP IQ/16.1 SP20 PL01/20080/P/MS/...");
	}

}
