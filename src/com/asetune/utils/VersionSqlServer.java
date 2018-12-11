package com.asetune.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL-Server version parser
 * <p>
 * 
 * The below version information is grabbed from: http://sqlserverversions.blogspot.se/
 */

public class VersionSqlServer
{
	// #################################################################
	// ## SQL Server 2016
	// ## I don't know the version string for those, so I'm just guessing it will be 13.0
	// #################################################################
	// +----------------------------+----------------+-----------------+
	// | Version                    | Build          | Release Date    |
	// +----------------------------+----------------+-----------------+
	// | no-info                    | no-info        | no-info         |
	// +----------------------------+----------------+-----------------+
	public static final long SQLSERVER_2016          = Ver.ver(13,0,0, 0, 0);

	// #################################################################
	// ## SQL Server 2014
	// #################################################################
	// +----------------------------+----------------+-----------------+
	// | Version                    | Build          | Release Date    |
	// +----------------------------+----------------+-----------------+
	// | Version                    | Build          | Release Date    |
	// | SP1 CU-3                   | 12.0.4427.24   | October 2015    |
	// | SP1 CU-2                   | 12.0.4422.0    | August 2015     |
	// | SP1 CU-1                   | 12.0.4416.0    | June 2015       |
	// | Service Pack 1 =========== | 12.0.4100.0    | May 2015        |
	// | CU-8                       | 12.0.2546.0    | June 2015       |
	// | CU-7                       | 12.0.2495.0    | April 2015      |
	// | CU-6                       | 12.0.2480.0    | February 2015   |
	// | CU-5                       | 12.0.2456.0    | December 2014   |
	// | CU-4                       | 12.0.2430.0    | October 2014    |
	// | CU-3                       | 12.0.2402.0    | August 2014     |
	// | CU-2                       | 12.0.2370.0    | June 2014       |
	// | CU-1                       | 12.0.2342.0    | April 2014      |
	// | RTM                        | 12.0.2000.0    | April 2014      |
	// +----------------------------+----------------+-----------------+
	public static final long SQLSERVER_2014_SP1_CU3  = Ver.ver(12,0,0, 1, 3);
	public static final long SQLSERVER_2014_SP1_CU2  = Ver.ver(12,0,0, 1, 2);
	public static final long SQLSERVER_2014_SP1_CU1  = Ver.ver(12,0,0, 1, 1);
	public static final long SQLSERVER_2014_SP1      = Ver.ver(12,0,0, 1, 0);
	public static final long SQLSERVER_2014_CU8      = Ver.ver(12,0,0, 0, 8);
	public static final long SQLSERVER_2014_CU7      = Ver.ver(12,0,0, 0, 7);
	public static final long SQLSERVER_2014_CU6      = Ver.ver(12,0,0, 0, 6);
	public static final long SQLSERVER_2014_CU5      = Ver.ver(12,0,0, 0, 5);
	public static final long SQLSERVER_2014_CU4      = Ver.ver(12,0,0, 0, 4);
	public static final long SQLSERVER_2014_CU3      = Ver.ver(12,0,0, 0, 3);
	public static final long SQLSERVER_2014_CU2      = Ver.ver(12,0,0, 0, 2);
	public static final long SQLSERVER_2014_CU1      = Ver.ver(12,0,0, 0, 1);
	public static final long SQLSERVER_2014          = Ver.ver(12,0,0, 0, 0);

	// #################################################################
	// ## SQL Server 2012
	// #################################################################
	// +----------------------------+----------------+-----------------+
	// | Version                    | Build          | Release Date    |
	// +----------------------------+----------------+-----------------+
	// | Service Pack 3 =========== | 11.0.6020.0    | November 2015   |
	// | SP2 CU-9                   | 11.0.5641.0    | November 2015   |
	// | SP2 CU-8                   | 11.0.5634.0    | September 2015  |
	// | SP2 CU-7                   | 11.0.5623.0    | July 2015       |
	// | SP2 CU-6                   | 11.0.5592.0    | May 2015        |
	// | SP2 CU-5                   | 11.0.5582.0    | March 2015      |
	// | SP2 CU-4                   | 11.0.5569.0    | January 2015    |
	// | SP2 CU-3                   | 11.0.5556.0    | November 2014   |
	// | SP2 CU-2                   | 11.0.5548.0    | September 2014  |
	// | SP2 CU-1                   | 11.0.5532.0    | July 2014       |
	// | Service Pack 2 =========== | 11.0.5058.0    | June 2014       |
	// | SP1 CU-13                  | 11.0.3482.0    | November 2014   |
	// | SP1 CU-12                  | 11.0.3470.0    | September 2014  |
	// | SP1 CU-11                  | 11.0.3449.0    | July 2014       |
	// | SP1 CU-10                  | 11.0.3431.0    | May 2014        |
	// | SP1 CU-9                   | 11.0.3412.0    | March 2014      |
	// | SP1 CU-8                   | 11.0.3401.0    | January 2014    |
	// | SP1 CU-7                   | 11.0.3393.0    | November 2013   |
	// | SP1 CU-6                   | 11.0.3381.0    | September 2013  |
	// | SP1 CU-5                   | 11.0.3373.0    | July 2013       |
	// | SP1 CU-4                   | 11.0.3368.0    | May 2013        |
	// | SP1 CU-3                   | 11.0.3349.0    | March 2013      |
	// | SP1 CU-2                   | 11.0.3339.0    | January 2013    |
	// | SP1 CU-1                   | 11.0.3321.0    | November 2012   |
	// | Service Pack 1 =========== | 11.0.3000.0    | November 2012   |
	// | CU-5                       | 11.0.2395.0    | December 2012   |
	// | CU-4                       | 11.0.2383.0    | September 2012  |
	// | CU-3                       | 11.0.2332.0    | August 2012     |
	// | CU-2                       | 11.0.2325.0    | June 2012       |
	// | CU-1                       | 11.0.2316.0    | April 2012      |
	// | RTM                        | 11.0.2100.60   | March 2011      |
	// +----------------------------+----------------+-----------------+
	public static final long SQLSERVER_2012_SP3      = Ver.ver(11,0,0, 3, 0);
	public static final long SQLSERVER_2012_SP2_CU9  = Ver.ver(11,0,0, 2, 9);
	public static final long SQLSERVER_2012_SP2_CU8  = Ver.ver(11,0,0, 2, 8);
	public static final long SQLSERVER_2012_SP2_CU7  = Ver.ver(11,0,0, 2, 7);
	public static final long SQLSERVER_2012_SP2_CU6  = Ver.ver(11,0,0, 2, 6);
	public static final long SQLSERVER_2012_SP2_CU5  = Ver.ver(11,0,0, 2, 5);
	public static final long SQLSERVER_2012_SP2_CU4  = Ver.ver(11,0,0, 2, 4);
	public static final long SQLSERVER_2012_SP2_CU3  = Ver.ver(11,0,0, 2, 3);
	public static final long SQLSERVER_2012_SP2_CU2  = Ver.ver(11,0,0, 2, 2);
	public static final long SQLSERVER_2012_SP2_CU1  = Ver.ver(11,0,0, 2, 1);
	public static final long SQLSERVER_2012_SP2      = Ver.ver(11,0,0, 2, 0);
	public static final long SQLSERVER_2012_SP1_CU13 = Ver.ver(11,0,0, 1, 13);
	public static final long SQLSERVER_2012_SP1_CU12 = Ver.ver(11,0,0, 1, 12);
	public static final long SQLSERVER_2012_SP1_CU11 = Ver.ver(11,0,0, 1, 11);
	public static final long SQLSERVER_2012_SP1_CU10 = Ver.ver(11,0,0, 1, 10);
	public static final long SQLSERVER_2012_SP1_CU9  = Ver.ver(11,0,0, 1, 9);
	public static final long SQLSERVER_2012_SP1_CU8  = Ver.ver(11,0,0, 1, 8);
	public static final long SQLSERVER_2012_SP1_CU7  = Ver.ver(11,0,0, 1, 7);
	public static final long SQLSERVER_2012_SP1_CU6  = Ver.ver(11,0,0, 1, 6);
	public static final long SQLSERVER_2012_SP1_CU5  = Ver.ver(11,0,0, 1, 5);
	public static final long SQLSERVER_2012_SP1_CU4  = Ver.ver(11,0,0, 1, 4);
	public static final long SQLSERVER_2012_SP1_CU3  = Ver.ver(11,0,0, 1, 3);
	public static final long SQLSERVER_2012_SP1_CU2  = Ver.ver(11,0,0, 1, 2);
	public static final long SQLSERVER_2012_SP1_CU1  = Ver.ver(11,0,0, 1, 1);
	public static final long SQLSERVER_2012_SP1      = Ver.ver(11,0,0, 1, 0);
	public static final long SQLSERVER_2012_CU5      = Ver.ver(11,0,0, 0, 5);
	public static final long SQLSERVER_2012_CU4      = Ver.ver(11,0,0, 0, 4);
	public static final long SQLSERVER_2012_CU3      = Ver.ver(11,0,0, 0, 3);
	public static final long SQLSERVER_2012_CU2      = Ver.ver(11,0,0, 0, 2);
	public static final long SQLSERVER_2012_CU1      = Ver.ver(11,0,0, 0, 1);
	public static final long SQLSERVER_2012          = Ver.ver(11,0,0, 0, 0);

	// #################################################################
	// ## SQL Server 2008 R2
	// #################################################################
	// +----------------------------+----------------+-----------------+
	// | Version                    | Build          | Release Date    |
	// +----------------------------+----------------+-----------------+
	// | Service Pack 3 =========== | 10.50.6000     | September 2014  |
	// | SP2 CU-13                  | 10.50.4319     | June 2014       |
	// | SP2 CU-12                  | 10.50.4305     | April 2014      |
	// | SP2 CU-11                  | 10.50.4302     | February 2014   |
	// | SP2 CU-10                  | 10.50.4297     | December 2013   |
	// | SP2 CU-9                   | 10.50.4295     | October 2013    |
	// | SP2 CU-8                   | 10.50.4290     | August 2013     |
	// | SP2 CU-7                   | 10.50.4286     | June 2013       |
	// | SP2 CU-6                   | 10.50.4279     | April 2013      |
	// | SP2 CU-5                   | 10.50.4276     | February 2013   |
	// | SP2 CU-4                   | 10.50.4270     | December 2012   |
	// | SP2 CU-3                   | 10.50.4266     | October 2012    |
	// | SP2 CU-2                   | 10.50.4263     | August 2012     |
	// | SP2 CU-1                   | 10.50.4260     | August 2012     |
	// | Service Pack 2 =========== | 10.50.4000     | July 2012       |
	// | SP1 CU-10                  | 10.50.2868     | December 2012   |
	// | SP1 CU-8                   | 10.50.2817     | August 2012     |
	// | SP1 CU-7                   | 10.50.2822     | June 2012       |
	// | SP1 CU-6                   | 10.50.2811     | April, 2012     |
	// | SP1 CU-5                   | 10.50.2806     | February 2012   |
	// | SP1 CU-4                   | 10.50.2796     | December 2011   |
	// | SP1 CU-3                   | 10.50.2789     | October 2011    |
	// | SP1 CU-2                   | 10.50.2772     | August 2011     |
	// | SP1 CU-1                   | 10.50.2769     | July 2011       |
	// | Service Pack 1 =========== | 10.50.2500     | July 2011       |
	// | RTM CU-14                  | 10.50.1817     | June 2012       |
	// | RTM CU-13                  | 10.50.1815     | April 2012      |
	// | RTM CU-12                  | 10.50.1810     | March 2012      |
	// | RTM CU-11                  | 10.50.1809     | February 2012   |
	// | RTM CU-10                  | 10.50.1807     | October 2011    |
	// | RTM CU-9                   | 10.50.1804     | August 2011     |
	// | RTM CU-8                   | 10.50.1797     | June 2011       |
	// | RTM CU-7                   | 10.50.1777     | April 2011      |
	// | RTM CU-6                   | 10.5.1765      | February 2011   |
	// | RTM CU-5                   | 10.5.1753      | December 2010   |
	// | RTM CU-4                   | 10.5.1746      | October 2010    |
	// | RTM CU-3                   | 10.5.1734      | August 2010     |
	// | RTM CU-2                   | 10.5.1720      | June 2010       |
	// | RTM CU-1                   | 10.5.1702      | May 2010        |
	// | RTM                        | 10.5.1600      | April 2010      |
	// +----------------------------+----------------+-----------------+
	public static final long SQLSERVER_2008R2_SP3      = Ver.ver(10,5,0, 3, 0);
	public static final long SQLSERVER_2008R2_SP2_CU13 = Ver.ver(10,5,0, 2, 13);
	public static final long SQLSERVER_2008R2_SP2_CU12 = Ver.ver(10,5,0, 2, 12);
	public static final long SQLSERVER_2008R2_SP2_CU11 = Ver.ver(10,5,0, 2, 11);
	public static final long SQLSERVER_2008R2_SP2_CU10 = Ver.ver(10,5,0, 2, 10);
	public static final long SQLSERVER_2008R2_SP2_CU9  = Ver.ver(10,5,0, 2, 9);
	public static final long SQLSERVER_2008R2_SP2_CU8  = Ver.ver(10,5,0, 2, 8);
	public static final long SQLSERVER_2008R2_SP2_CU7  = Ver.ver(10,5,0, 2, 7);
	public static final long SQLSERVER_2008R2_SP2_CU6  = Ver.ver(10,5,0, 2, 6);
	public static final long SQLSERVER_2008R2_SP2_CU5  = Ver.ver(10,5,0, 2, 5);
	public static final long SQLSERVER_2008R2_SP2_CU4  = Ver.ver(10,5,0, 2, 4);
	public static final long SQLSERVER_2008R2_SP2_CU3  = Ver.ver(10,5,0, 2, 3);
	public static final long SQLSERVER_2008R2_SP2_CU2  = Ver.ver(10,5,0, 2, 2);
	public static final long SQLSERVER_2008R2_SP2_CU1  = Ver.ver(10,5,0, 2, 1);
	public static final long SQLSERVER_2008R2_SP2      = Ver.ver(10,5,0, 2, 0);
	public static final long SQLSERVER_2008R2_SP1_CU10 = Ver.ver(10,5,0, 1, 10);
	public static final long SQLSERVER_2008R2_SP1_CU9  = Ver.ver(10,5,0, 1, 9);
	public static final long SQLSERVER_2008R2_SP1_CU8  = Ver.ver(10,5,0, 1, 8);
	public static final long SQLSERVER_2008R2_SP1_CU7  = Ver.ver(10,5,0, 1, 7);
	public static final long SQLSERVER_2008R2_SP1_CU6  = Ver.ver(10,5,0, 1, 6);
	public static final long SQLSERVER_2008R2_SP1_CU5  = Ver.ver(10,5,0, 1, 5);
	public static final long SQLSERVER_2008R2_SP1_CU4  = Ver.ver(10,5,0, 1, 4);
	public static final long SQLSERVER_2008R2_SP1_CU3  = Ver.ver(10,5,0, 1, 3);
	public static final long SQLSERVER_2008R2_SP1_CU2  = Ver.ver(10,5,0, 1, 2);
	public static final long SQLSERVER_2008R2_SP1_CU1  = Ver.ver(10,5,0, 1, 1);
	public static final long SQLSERVER_2008R2_SP1      = Ver.ver(10,5,0, 1, 0);
	public static final long SQLSERVER_2008R2_CU14     = Ver.ver(10,5,0, 0, 14);
	public static final long SQLSERVER_2008R2_CU13     = Ver.ver(10,5,0, 0, 13);
	public static final long SQLSERVER_2008R2_CU12     = Ver.ver(10,5,0, 0, 12);
	public static final long SQLSERVER_2008R2_CU11     = Ver.ver(10,5,0, 0, 11);
	public static final long SQLSERVER_2008R2_CU10     = Ver.ver(10,5,0, 0, 10);
	public static final long SQLSERVER_2008R2_CU9      = Ver.ver(10,5,0, 0, 9);
	public static final long SQLSERVER_2008R2_CU8      = Ver.ver(10,5,0, 0, 8);
	public static final long SQLSERVER_2008R2_CU7      = Ver.ver(10,5,0, 0, 7);
	public static final long SQLSERVER_2008R2_CU6      = Ver.ver(10,5,0, 0, 6);
	public static final long SQLSERVER_2008R2_CU5      = Ver.ver(10,5,0, 0, 5);
	public static final long SQLSERVER_2008R2_CU4      = Ver.ver(10,5,0, 0, 4);
	public static final long SQLSERVER_2008R2_CU3      = Ver.ver(10,5,0, 0, 3);
	public static final long SQLSERVER_2008R2_CU2      = Ver.ver(10,5,0, 0, 2);
	public static final long SQLSERVER_2008R2_CU1      = Ver.ver(10,5,0, 0, 1);
	public static final long SQLSERVER_2008R2          = Ver.ver(10,5,0, 0, 0);

	// #################################################################
	// ## SQL Server 2008
	// #################################################################
	// +----------------------------+----------------+-----------------+
	// | Version                    | Build          | Release Date    |
	// +----------------------------+----------------+-----------------+
	// | Service Pack 4 =========== | 10.00.6000     | May 2014        |
	// | SP3 CU-17                  | 10.00.5861     | May 2014        |
	// | SP3 CU-16                  | 10.00.5852     | March 2014      |
	// | SP3 CU-15                  | 10.00.5850     | January 2014    |
	// | SP3 CU-14                  | 10.00.5848     | November 2013   |
	// | SP3 CU-13                  | 10.00.5846     | September 2013  |
	// | SP3 CU-12                  | 10.00.5844     | July 2013       |
	// | SP3 CU-11                  | 10.00.5840     | May 2013        |
	// | SP3 CU-10                  | 10.00.5835     | March 2013      |
	// | SP3 CU-9                   | 10.00.5829     | January 2013    |
	// | SP3 CU-8                   | 10.00.5828     | November 2012   |
	// | SP3 CU-7                   | 10.00.5794     | September 2012  |
	// | SP3 CU-6                   | 10.00.5788     | July 2012       |
	// | SP3 CU-5                   | 10.00.5785     | May 2012        |
	// | SP3 CU-4                   | 10.00.5775     | March 2012      |
	// | SP3 CU-3                   | 10.00.5770     | Januaury 2012   |
	// | SP3 CU-2                   | 10.00.5768     | November 2011   |
	// | SP3 CU-1                   | 10.00.5766     | October 2011    |
	// | Service Pack 3 =========== | 10.00.5500     | October 2011    |
	// | SP2 CU-10                  | 10.00.4332     | May 2012        |
	// | SP2 CU-9                   | 10.00.4330     | March 2012      |
	// | SP2 CU-8                   | 10.00.4326     | January 2012    |
	// | SP2 CU-7                   | 10.00.4323     | November 2011   |
	// | SP2 CU-6                   | 10.00.4321     | September 2011  |
	// | SP2 CU-5                   | 10.00.4316     | July 2011       |
	// | SP2 CU-4                   | 10.00.4285     | May 2011        |
	// | SP2 CU-3                   | 10.00.4279     | March 2011      |
	// | SP2 CU-2                   | 10.00.4272     | January 2011    |
	// | SP2 CU-1                   | 10.00.4266     | November 2010   |
	// | Service Pack 2 =========== | 10.00.4000     | September 2010  |
	// | SP1 CU-16                  | 10.00.2850     | September 2011  |
	// | SP1 CU-15                  | 10.00.2847     | July 2011       |
	// | SP1 CU-14                  | 10.00.2821     | May 2011        |
	// | SP1 CU-13                  | 10.00.2816     | March 2011      |
	// | SP1 CU-12                  | 10.00.2808     | January 2011    |
	// | SP1 CU-11                  | 10.00.2804     | November 2010   |
	// | SP1 CU-10                  | 10.00.2799     | September 2010  |
	// | SP1 CU-9                   | 10.00.2789     | July 2010       |
	// | SP1 CU-8                   | 10.00.2775     | May 2010        |
	// | SP1 CU-7                   | 10.00.2766     | March 2010      |
	// | SP1 CU-6                   | 10.00.2757     | January 2010    |
	// | SP1 CU-5                   | 10.00.2746     | November 2009   |
	// | SP1 CU-4                   | 10.00.2734     | September 2009  |
	// | SP1 CU-3                   | 10.00.2723     | July 2009       |
	// | SP1 CU-2                   | 10.00.2714     | May 2009        |
	// | SP1 CU-1                   | 10.00.2710     | April 2009      |
	// | Service Pack 1 =========== | 10.00.2531     | April 2009      |
	// | RTM CU-10                  | 10.00.1835     | March 2010      |
	// | RTM CU-9                   | 10.00.1828     | January 2010    |
	// | RTM CU-8                   | 10.00.1823     | November 2009   |
	// | RTM CU-7                   | 10.00.1818     | September 2009  |
	// | RTM CU-6                   | 10.00.1812     | July 2009       |
	// | RTM CU-5                   | 10.00.1806     | May 2009        |
	// | RTM CU-4                   | 10.00.1798     | March 2009      |
	// | RTM CU-3                   | 10.00.1787     | January 2009    |
	// | RTM CU-2                   | 10.00.1779     | November 2008   |
	// | RTM CU-1                   | 10.00.1763     | November 2008   |
	// | RTM                        | 10.00.1600     | August 2008     |
	// +----------------------------+----------------+-----------------+
	public static final long SQLSERVER_2008_SP4      = Ver.ver(10,0,0, 4, 0);
	public static final long SQLSERVER_2008_SP3_CU17 = Ver.ver(10,0,0, 3, 17);
	public static final long SQLSERVER_2008_SP3_CU16 = Ver.ver(10,0,0, 3, 16);
	public static final long SQLSERVER_2008_SP3_CU15 = Ver.ver(10,0,0, 3, 15);
	public static final long SQLSERVER_2008_SP3_CU14 = Ver.ver(10,0,0, 3, 14);
	public static final long SQLSERVER_2008_SP3_CU13 = Ver.ver(10,0,0, 3, 13);
	public static final long SQLSERVER_2008_SP3_CU12 = Ver.ver(10,0,0, 3, 12);
	public static final long SQLSERVER_2008_SP3_CU11 = Ver.ver(10,0,0, 3, 11);
	public static final long SQLSERVER_2008_SP3_CU10 = Ver.ver(10,0,0, 3, 10);
	public static final long SQLSERVER_2008_SP3_CU9  = Ver.ver(10,0,0, 3, 9);
	public static final long SQLSERVER_2008_SP3_CU8  = Ver.ver(10,0,0, 3, 8);
	public static final long SQLSERVER_2008_SP3_CU7  = Ver.ver(10,0,0, 3, 7);
	public static final long SQLSERVER_2008_SP3_CU6  = Ver.ver(10,0,0, 3, 6);
	public static final long SQLSERVER_2008_SP3_CU5  = Ver.ver(10,0,0, 3, 5);
	public static final long SQLSERVER_2008_SP3_CU4  = Ver.ver(10,0,0, 3, 4);
	public static final long SQLSERVER_2008_SP3_CU3  = Ver.ver(10,0,0, 3, 3);
	public static final long SQLSERVER_2008_SP3_CU2  = Ver.ver(10,0,0, 3, 2);
	public static final long SQLSERVER_2008_SP3_CU1  = Ver.ver(10,0,0, 3, 1);
	public static final long SQLSERVER_2008_SP3      = Ver.ver(10,0,0, 3, 0);
	public static final long SQLSERVER_2008_SP2_CU10 = Ver.ver(10,0,0, 2, 10);
	public static final long SQLSERVER_2008_SP2_CU9  = Ver.ver(10,0,0, 2, 9);
	public static final long SQLSERVER_2008_SP2_CU8  = Ver.ver(10,0,0, 2, 8);
	public static final long SQLSERVER_2008_SP2_CU7  = Ver.ver(10,0,0, 2, 7);
	public static final long SQLSERVER_2008_SP2_CU6  = Ver.ver(10,0,0, 2, 6);
	public static final long SQLSERVER_2008_SP2_CU5  = Ver.ver(10,0,0, 2, 5);
	public static final long SQLSERVER_2008_SP2_CU4  = Ver.ver(10,0,0, 2, 4);
	public static final long SQLSERVER_2008_SP2_CU3  = Ver.ver(10,0,0, 2, 3);
	public static final long SQLSERVER_2008_SP2_CU2  = Ver.ver(10,0,0, 2, 2);
	public static final long SQLSERVER_2008_SP2_CU1  = Ver.ver(10,0,0, 2, 1);
	public static final long SQLSERVER_2008_SP2      = Ver.ver(10,0,0, 2, 0);
	public static final long SQLSERVER_2008_SP1_CU16 = Ver.ver(10,0,0, 1, 16);
	public static final long SQLSERVER_2008_SP1_CU15 = Ver.ver(10,0,0, 1, 15);
	public static final long SQLSERVER_2008_SP1_CU14 = Ver.ver(10,0,0, 1, 14);
	public static final long SQLSERVER_2008_SP1_CU13 = Ver.ver(10,0,0, 1, 13);
	public static final long SQLSERVER_2008_SP1_CU12 = Ver.ver(10,0,0, 1, 12);
	public static final long SQLSERVER_2008_SP1_CU11 = Ver.ver(10,0,0, 1, 11);
	public static final long SQLSERVER_2008_SP1_CU10 = Ver.ver(10,0,0, 1, 10);
	public static final long SQLSERVER_2008_SP1_CU9  = Ver.ver(10,0,0, 1, 9);
	public static final long SQLSERVER_2008_SP1_CU8  = Ver.ver(10,0,0, 1, 8);
	public static final long SQLSERVER_2008_SP1_CU7  = Ver.ver(10,0,0, 1, 7);
	public static final long SQLSERVER_2008_SP1_CU6  = Ver.ver(10,0,0, 1, 6);
	public static final long SQLSERVER_2008_SP1_CU5  = Ver.ver(10,0,0, 1, 5);
	public static final long SQLSERVER_2008_SP1_CU4  = Ver.ver(10,0,0, 1, 4);
	public static final long SQLSERVER_2008_SP1_CU3  = Ver.ver(10,0,0, 1, 3);
	public static final long SQLSERVER_2008_SP1_CU2  = Ver.ver(10,0,0, 1, 2);
	public static final long SQLSERVER_2008_SP1_CU1  = Ver.ver(10,0,0, 1, 1);
	public static final long SQLSERVER_2008_SP1      = Ver.ver(10,0,0, 1, 0);
	public static final long SQLSERVER_2008_CU10     = Ver.ver(10,0,0, 0, 10);
	public static final long SQLSERVER_2008_CU9      = Ver.ver(10,0,0, 0, 9);
	public static final long SQLSERVER_2008_CU8      = Ver.ver(10,0,0, 0, 8);
	public static final long SQLSERVER_2008_CU7      = Ver.ver(10,0,0, 0, 7);
	public static final long SQLSERVER_2008_CU6      = Ver.ver(10,0,0, 0, 6);
	public static final long SQLSERVER_2008_CU5      = Ver.ver(10,0,0, 0, 5);
	public static final long SQLSERVER_2008_CU4      = Ver.ver(10,0,0, 0, 4);
	public static final long SQLSERVER_2008_CU3      = Ver.ver(10,0,0, 0, 3);
	public static final long SQLSERVER_2008_CU2      = Ver.ver(10,0,0, 0, 2);
	public static final long SQLSERVER_2008_CU1      = Ver.ver(10,0,0, 0, 1);
	public static final long SQLSERVER_2008          = Ver.ver(10,0,0, 0, 0);

	// #################################################################
	// ## SQL Server 2005
	// #################################################################
	// +----------------------------+----------------+-----------------+
	// | Version                    | Build          | Release Date    |
	// +----------------------------+----------------+-----------------+
	// | SP4 CU-3                   | 9.00.5266      | March 2011      |
	// | SP4 CU-2                   | 9.00.5259      | February 2011   |
	// | SP4 CU-1                   | 9.00.5254      | December 2010   |
	// | Service Pack 4 =========== | 9.00.5000      | December 2010   |
	// | SP3 CU-15                  | 9.00.4325      | March 2011      |
	// | SP3 CU-14                  | 9.00.4317      | February 2011   |
	// | SP3 CU-13                  | 9.00.4315      | December 2010   |
	// | SP3 CU-12                  | 9.00.4311      | October 2010    |
	// | SP3 CU-11                  | 9.00.4309      | August 2010     |
	// | SP3 CU-10                  | 9.00.4305      | June 2010       |
	// | SP3 CU-9                   | 9.00.4294      | April 2010      |
	// | SP3 CU-8                   | 9.00.4285      | February 2010   |
	// | SP3 CU-7                   | 9.00.4273      | December 2009   |
	// | SP3 CU-6                   | 9.00.4266      | October 2009    |
	// | SP3 CU-5                   | 9.00.4230      | August 2009     |
	// | SP3 CU-4                   | 9.00.4226      | June 2009       |
	// | SP3 CU-3                   | 9.00.4220      | April 2009      |
	// | SP3 CU-2                   | 9.00.4211      | February 2009   |
	// | SP3 CU-1                   | 9.00.4207      | December 2008   |
	// | Service Pack 3 =========== | 9.00.4035      | December 2008   |
	// | SP2 CU-17                  | 9.00.3356      | December 2009   |
	// | SP2 CU-16                  | 9.00.3355      | October 2009    |
	// | SP2 CU-15                  | 9.00.3330      | August 2009     |
	// | SP2 CU-14                  | 9.00.3328      | June 2009       |
	// | SP2 CU-13                  | 9.00.3325      | March 2009      |
	// | SP2 CU-12                  | 9.00.3315      | February 2009   |
	// | SP2 CU-11                  | 9.00.3301      | December 2008   |
	// | SP2 CU-10                  | 9.00.3294      | October 2008    |
	// | SP2 CU-9                   | 9.00.3282      | August 2008     |
	// | SP2 CU-8                   | 9.00.3257      | June 2008       |
	// | SP2 CU-7                   | 9.00.3239      | April 2008      |
	// | SP2 CU-6                   | 9.00.3228      | February 2008   |
	// | SP2 CU-5                   | 9.00.3215      | December 2007   |
	// | SP2 CU-4                   | 9.00.3200      | October 2007    |
	// | SP2 CU-3                   | 9.00.3186      | August 2007     |
	// | SP2 CU-2                   | 9.00.3175      | June 2007       |
	// | SP2 CU-1                   | 9.00.3161      | April 2007      |
	// | Service Pack 2 =========== | 9.00.3042      | February 2007   |
	// | SP1 CU                     | 9.00.2153      | September 2006  |
	// | Service Pack 1 =========== | 9.00.2047      | April 2006      |
	// | RTM                        | 9.00.1399      | November 2005   |
	// +----------------------------+----------------+-----------------+
	public static final long SQLSERVER_2005_SP4_CU3  = Ver.ver(9,0,0, 4, 3);
	public static final long SQLSERVER_2005_SP4_CU2  = Ver.ver(9,0,0, 4, 2);
	public static final long SQLSERVER_2005_SP4_CU1  = Ver.ver(9,0,0, 4, 1);
	public static final long SQLSERVER_2005_SP4      = Ver.ver(9,0,0, 4, 0);
	public static final long SQLSERVER_2005_SP3_CU15 = Ver.ver(9,0,0, 3, 15);
	public static final long SQLSERVER_2005_SP3_CU14 = Ver.ver(9,0,0, 3, 14);
	public static final long SQLSERVER_2005_SP3_CU13 = Ver.ver(9,0,0, 3, 13);
	public static final long SQLSERVER_2005_SP3_CU12 = Ver.ver(9,0,0, 3, 12);
	public static final long SQLSERVER_2005_SP3_CU11 = Ver.ver(9,0,0, 3, 11);
	public static final long SQLSERVER_2005_SP3_CU10 = Ver.ver(9,0,0, 3, 10);
	public static final long SQLSERVER_2005_SP3_CU9  = Ver.ver(9,0,0, 3, 9);
	public static final long SQLSERVER_2005_SP3_CU8  = Ver.ver(9,0,0, 3, 8);
	public static final long SQLSERVER_2005_SP3_CU7  = Ver.ver(9,0,0, 3, 7);
	public static final long SQLSERVER_2005_SP3_CU6  = Ver.ver(9,0,0, 3, 6);
	public static final long SQLSERVER_2005_SP3_CU5  = Ver.ver(9,0,0, 3, 5);
	public static final long SQLSERVER_2005_SP3_CU4  = Ver.ver(9,0,0, 3, 4);
	public static final long SQLSERVER_2005_SP3_CU3  = Ver.ver(9,0,0, 3, 3);
	public static final long SQLSERVER_2005_SP3_CU2  = Ver.ver(9,0,0, 3, 2);
	public static final long SQLSERVER_2005_SP3_CU1  = Ver.ver(9,0,0, 3, 1);
	public static final long SQLSERVER_2005_SP3      = Ver.ver(9,0,0, 3, 0);
	public static final long SQLSERVER_2005_SP2_CU17 = Ver.ver(9,0,0, 2, 17);
	public static final long SQLSERVER_2005_SP2_CU16 = Ver.ver(9,0,0, 2, 16);
	public static final long SQLSERVER_2005_SP2_CU15 = Ver.ver(9,0,0, 2, 15);
	public static final long SQLSERVER_2005_SP2_CU14 = Ver.ver(9,0,0, 2, 14);
	public static final long SQLSERVER_2005_SP2_CU13 = Ver.ver(9,0,0, 2, 13);
	public static final long SQLSERVER_2005_SP2_CU12 = Ver.ver(9,0,0, 2, 12);
	public static final long SQLSERVER_2005_SP2_CU11 = Ver.ver(9,0,0, 2, 11);
	public static final long SQLSERVER_2005_SP2_CU10 = Ver.ver(9,0,0, 2, 10);
	public static final long SQLSERVER_2005_SP2_CU9  = Ver.ver(9,0,0, 2, 9);
	public static final long SQLSERVER_2005_SP2_CU8  = Ver.ver(9,0,0, 2, 8);
	public static final long SQLSERVER_2005_SP2_CU7  = Ver.ver(9,0,0, 2, 7);
	public static final long SQLSERVER_2005_SP2_CU6  = Ver.ver(9,0,0, 2, 6);
	public static final long SQLSERVER_2005_SP2_CU5  = Ver.ver(9,0,0, 2, 5);
	public static final long SQLSERVER_2005_SP2_CU4  = Ver.ver(9,0,0, 2, 4);
	public static final long SQLSERVER_2005_SP2_CU3  = Ver.ver(9,0,0, 2, 3);
	public static final long SQLSERVER_2005_SP2_CU2  = Ver.ver(9,0,0, 2, 2);
	public static final long SQLSERVER_2005_SP2_CU1  = Ver.ver(9,0,0, 2, 1);
	public static final long SQLSERVER_2005_SP2      = Ver.ver(9,0,0, 2, 0);
	public static final long SQLSERVER_2005_SP1_CU1  = Ver.ver(9,0,0, 1, 1);
	public static final long SQLSERVER_2005_SP1      = Ver.ver(9,0,0, 1, 0);
	public static final long SQLSERVER_2005          = Ver.ver(9,0,0, 0, 0);

	// #################################################################
	// ## SQL Server 2000
	// #################################################################
	// +----------------------------+----------------+-----------------+
	// | Version                    | Build          | Release Date    |
	// +----------------------------+----------------+-----------------+
	// | SP4 CU                     | 8.00.2187      | October 2006    |
	// | Service Pack 4 =========== | 8.00.2039      | May 2005        |
	// | Service Pack 3 =========== | 8.00.760       | August 2003     |
	// | Service Pack 2 =========== | 8.00.532       | February 2003   |
	// | Service Pack 1 =========== | 8.00.384       | June 2001       |
	// | RTM                        | 8.00.14        | November 2000   |
	// +----------------------------+----------------+-----------------+
	public static final long SQLSERVER_2000_SP4_CU1  = Ver.ver(8,0,0, 4, 1);
	public static final long SQLSERVER_2000_SP4      = Ver.ver(8,0,0, 4, 0);
	public static final long SQLSERVER_2000_SP3      = Ver.ver(8,0,0, 3, 0);
	public static final long SQLSERVER_2000_SP2      = Ver.ver(8,0,0, 2, 0);
	public static final long SQLSERVER_2000_SP1      = Ver.ver(8,0,0, 1, 0);
	public static final long SQLSERVER_2000          = Ver.ver(8,0,0, 0, 0);
	
	
	public static long parseVersionStringToNumber(String versionStr)
	{
		return parse(versionStr);
	}

	private static long parse(String version)
	{
		long intVersion = -1;
		
		String regexp = "(\\d+)\\.(\\d+)\\.(\\d+)(?:\\.(\\d+))?"; // search for versions like 1.2[.3]
        Pattern versionPattern = Pattern.compile(regexp);
        Matcher m = versionPattern.matcher(version);
		if (m.find())
		{
			String g1str = m.group(1);
			String g2str = m.group(2);
			String g3str = m.group(3);
			String g4str = m.group(4);
			
			if (g1str == null) g1str = "0";
			if (g2str == null) g2str = "0";
			if (g3str == null) g3str = "0";
			if (g4str == null) g4str = "0";
			
			long v1 = Long.parseLong(g1str);
			long v2 = Long.parseLong(g2str);
			long v3 = Long.parseLong(g3str);
			long v4 = Long.parseLong(g4str);

			// earlier version than SQL-Server 2000: NOT Supported
			if (v1 < 8)
				return 0;

			// #################################################################
			// ## SQL Server 2000
			// #################################################################
			// +----------------------------+----------------+-----------------+
			// | Version                    | Build          | Release Date    |
			// +----------------------------+----------------+-----------------+
			// | SP4 CU                     | 8.00.2187      | October 2006    |
			// | Service Pack 4 =========== | 8.00.2039      | May 2005        |
			// | Service Pack 3 =========== | 8.00.760       | August 2003     |
			// | Service Pack 2 =========== | 8.00.532       | February 2003   |
			// | Service Pack 1 =========== | 8.00.384       | June 2001       |
			// | RTM                        | 8.00.14        | November 2000   |
			// +----------------------------+----------------+-----------------+
			if (v1 == 8 && v2 == 0)
			{
    			if (v3 >= 2187) return SQLSERVER_2000_SP4_CU1;
    			if (v3 >= 2039) return SQLSERVER_2000_SP4;
    			if (v3 >= 760 ) return SQLSERVER_2000_SP3;
    			if (v3 >= 532 ) return SQLSERVER_2000_SP2;
    			if (v3 >= 384 ) return SQLSERVER_2000_SP1;
    			if (v3 >= 14  ) return SQLSERVER_2000;
			}

			// #################################################################
			// ## SQL Server 2005
			// #################################################################
			// +----------------------------+----------------+-----------------+
			// | Version                    | Build          | Release Date    |
			// +----------------------------+----------------+-----------------+
			// | SP4 CU-3                   | 9.00.5266      | March 2011      |
			// | SP4 CU-2                   | 9.00.5259      | February 2011   |
			// | SP4 CU-1                   | 9.00.5254      | December 2010   |
			// | Service Pack 4 =========== | 9.00.5000      | December 2010   |
			// | SP3 CU-15                  | 9.00.4325      | March 2011      |
			// | SP3 CU-14                  | 9.00.4317      | February 2011   |
			// | SP3 CU-13                  | 9.00.4315      | December 2010   |
			// | SP3 CU-12                  | 9.00.4311      | October 2010    |
			// | SP3 CU-11                  | 9.00.4309      | August 2010     |
			// | SP3 CU-10                  | 9.00.4305      | June 2010       |
			// | SP3 CU-9                   | 9.00.4294      | April 2010      |
			// | SP3 CU-8                   | 9.00.4285      | February 2010   |
			// | SP3 CU-7                   | 9.00.4273      | December 2009   |
			// | SP3 CU-6                   | 9.00.4266      | October 2009    |
			// | SP3 CU-5                   | 9.00.4230      | August 2009     |
			// | SP3 CU-4                   | 9.00.4226      | June 2009       |
			// | SP3 CU-3                   | 9.00.4220      | April 2009      |
			// | SP3 CU-2                   | 9.00.4211      | February 2009   |
			// | SP3 CU-1                   | 9.00.4207      | December 2008   |
			// | Service Pack 3 =========== | 9.00.4035      | December 2008   |
			// | SP2 CU-17                  | 9.00.3356      | December 2009   |
			// | SP2 CU-16                  | 9.00.3355      | October 2009    |
			// | SP2 CU-15                  | 9.00.3330      | August 2009     |
			// | SP2 CU-14                  | 9.00.3328      | June 2009       |
			// | SP2 CU-13                  | 9.00.3325      | March 2009      |
			// | SP2 CU-12                  | 9.00.3315      | February 2009   |
			// | SP2 CU-11                  | 9.00.3301      | December 2008   |
			// | SP2 CU-10                  | 9.00.3294      | October 2008    |
			// | SP2 CU-9                   | 9.00.3282      | August 2008     |
			// | SP2 CU-8                   | 9.00.3257      | June 2008       |
			// | SP2 CU-7                   | 9.00.3239      | April 2008      |
			// | SP2 CU-6                   | 9.00.3228      | February 2008   |
			// | SP2 CU-5                   | 9.00.3215      | December 2007   |
			// | SP2 CU-4                   | 9.00.3200      | October 2007    |
			// | SP2 CU-3                   | 9.00.3186      | August 2007     |
			// | SP2 CU-2                   | 9.00.3175      | June 2007       |
			// | SP2 CU-1                   | 9.00.3161      | April 2007      |
			// | Service Pack 2 =========== | 9.00.3042      | February 2007   |
			// | SP1 CU                     | 9.00.2153      | September 2006  |
			// | Service Pack 1 =========== | 9.00.2047      | April 2006      |
			// | RTM                        | 9.00.1399      | November 2005   |
			// +----------------------------+----------------+-----------------+
			else if (v1 == 9 && v2 == 0)
			{
    			if (v3 >= 5266) return SQLSERVER_2005_SP4_CU3;
    			if (v3 >= 5259) return SQLSERVER_2005_SP4_CU2;
    			if (v3 >= 5254) return SQLSERVER_2005_SP4_CU1;
    			if (v3 >= 5000) return SQLSERVER_2005_SP4;
    			if (v3 >= 4325) return SQLSERVER_2005_SP3_CU15;
    			if (v3 >= 4317) return SQLSERVER_2005_SP3_CU14;
    			if (v3 >= 4315) return SQLSERVER_2005_SP3_CU13;
    			if (v3 >= 4311) return SQLSERVER_2005_SP3_CU12;
    			if (v3 >= 4309) return SQLSERVER_2005_SP3_CU11;
    			if (v3 >= 4305) return SQLSERVER_2005_SP3_CU10;
    			if (v3 >= 4294) return SQLSERVER_2005_SP3_CU9;
    			if (v3 >= 4285) return SQLSERVER_2005_SP3_CU8;
    			if (v3 >= 4273) return SQLSERVER_2005_SP3_CU7;
    			if (v3 >= 4266) return SQLSERVER_2005_SP3_CU6;
    			if (v3 >= 4230) return SQLSERVER_2005_SP3_CU5;
    			if (v3 >= 4226) return SQLSERVER_2005_SP3_CU4;
    			if (v3 >= 4220) return SQLSERVER_2005_SP3_CU3;
    			if (v3 >= 4211) return SQLSERVER_2005_SP3_CU2;
    			if (v3 >= 4207) return SQLSERVER_2005_SP3_CU1;
    			if (v3 >= 4035) return SQLSERVER_2005_SP3;
    			if (v3 >= 3356) return SQLSERVER_2005_SP2_CU17;
    			if (v3 >= 3355) return SQLSERVER_2005_SP2_CU16;
    			if (v3 >= 3330) return SQLSERVER_2005_SP2_CU15;
    			if (v3 >= 3328) return SQLSERVER_2005_SP2_CU14;
    			if (v3 >= 3325) return SQLSERVER_2005_SP2_CU13;
    			if (v3 >= 3315) return SQLSERVER_2005_SP2_CU12;
    			if (v3 >= 3301) return SQLSERVER_2005_SP2_CU11;
    			if (v3 >= 3294) return SQLSERVER_2005_SP2_CU10;
    			if (v3 >= 3282) return SQLSERVER_2005_SP2_CU9;
    			if (v3 >= 3257) return SQLSERVER_2005_SP2_CU8;
    			if (v3 >= 3239) return SQLSERVER_2005_SP2_CU7;
    			if (v3 >= 3228) return SQLSERVER_2005_SP2_CU6;
    			if (v3 >= 3215) return SQLSERVER_2005_SP2_CU5;
    			if (v3 >= 3200) return SQLSERVER_2005_SP2_CU4;
    			if (v3 >= 3186) return SQLSERVER_2005_SP2_CU3;
    			if (v3 >= 3175) return SQLSERVER_2005_SP2_CU2;
    			if (v3 >= 3161) return SQLSERVER_2005_SP2_CU1;
    			if (v3 >= 3042) return SQLSERVER_2005_SP2;
    			if (v3 >= 2153) return SQLSERVER_2005_SP1_CU1;
    			if (v3 >= 2047) return SQLSERVER_2005_SP1;
    			if (v3 >= 1399) return SQLSERVER_2005;
			}

			// #################################################################
			// ## SQL Server 2008
			// #################################################################
			// +----------------------------+----------------+-----------------+
			// | Version                    | Build          | Release Date    |
			// +----------------------------+----------------+-----------------+
			// | Service Pack 4 =========== | 10.00.6000     | May 2014        |
			// | SP3 CU-17                  | 10.00.5861     | May 2014        |
			// | SP3 CU-16                  | 10.00.5852     | March 2014      |
			// | SP3 CU-15                  | 10.00.5850     | January 2014    |
			// | SP3 CU-14                  | 10.00.5848     | November 2013   |
			// | SP3 CU-13                  | 10.00.5846     | September 2013  |
			// | SP3 CU-12                  | 10.00.5844     | July 2013       |
			// | SP3 CU-11                  | 10.00.5840     | May 2013        |
			// | SP3 CU-10                  | 10.00.5835     | March 2013      |
			// | SP3 CU-9                   | 10.00.5829     | January 2013    |
			// | SP3 CU-8                   | 10.00.5828     | November 2012   |
			// | SP3 CU-7                   | 10.00.5794     | September 2012  |
			// | SP3 CU-6                   | 10.00.5788     | July 2012       |
			// | SP3 CU-5                   | 10.00.5785     | May 2012        |
			// | SP3 CU-4                   | 10.00.5775     | March 2012      |
			// | SP3 CU-3                   | 10.00.5770     | Januaury 2012   |
			// | SP3 CU-2                   | 10.00.5768     | November 2011   |
			// | SP3 CU-1                   | 10.00.5766     | October 2011    |
			// | Service Pack 3 =========== | 10.00.5500     | October 2011    |
			// | SP2 CU-10                  | 10.00.4332     | May 2012        |
			// | SP2 CU-9                   | 10.00.4330     | March 2012      |
			// | SP2 CU-8                   | 10.00.4326     | January 2012    |
			// | SP2 CU-7                   | 10.00.4323     | November 2011   |
			// | SP2 CU-6                   | 10.00.4321     | September 2011  |
			// | SP2 CU-5                   | 10.00.4316     | July 2011       |
			// | SP2 CU-4                   | 10.00.4285     | May 2011        |
			// | SP2 CU-3                   | 10.00.4279     | March 2011      |
			// | SP2 CU-2                   | 10.00.4272     | January 2011    |
			// | SP2 CU-1                   | 10.00.4266     | November 2010   |
			// | Service Pack 2 =========== | 10.00.4000     | September 2010  |
			// | SP1 CU-16                  | 10.00.2850     | September 2011  |
			// | SP1 CU-15                  | 10.00.2847     | July 2011       |
			// | SP1 CU-14                  | 10.00.2821     | May 2011        |
			// | SP1 CU-13                  | 10.00.2816     | March 2011      |
			// | SP1 CU-12                  | 10.00.2808     | January 2011    |
			// | SP1 CU-11                  | 10.00.2804     | November 2010   |
			// | SP1 CU-10                  | 10.00.2799     | September 2010  |
			// | SP1 CU-9                   | 10.00.2789     | July 2010       |
			// | SP1 CU-8                   | 10.00.2775     | May 2010        |
			// | SP1 CU-7                   | 10.00.2766     | March 2010      |
			// | SP1 CU-6                   | 10.00.2757     | January 2010    |
			// | SP1 CU-5                   | 10.00.2746     | November 2009   |
			// | SP1 CU-4                   | 10.00.2734     | September 2009  |
			// | SP1 CU-3                   | 10.00.2723     | July 2009       |
			// | SP1 CU-2                   | 10.00.2714     | May 2009        |
			// | SP1 CU-1                   | 10.00.2710     | April 2009      |
			// | Service Pack 1 =========== | 10.00.2531     | April 2009      |
			// | RTM CU-10                  | 10.00.1835     | March 2010      |
			// | RTM CU-9                   | 10.00.1828     | January 2010    |
			// | RTM CU-8                   | 10.00.1823     | November 2009   |
			// | RTM CU-7                   | 10.00.1818     | September 2009  |
			// | RTM CU-6                   | 10.00.1812     | July 2009       |
			// | RTM CU-5                   | 10.00.1806     | May 2009        |
			// | RTM CU-4                   | 10.00.1798     | March 2009      |
			// | RTM CU-3                   | 10.00.1787     | January 2009    |
			// | RTM CU-2                   | 10.00.1779     | November 2008   |
			// | RTM CU-1                   | 10.00.1763     | November 2008   |
			// | RTM                        | 10.00.1600     | August 2008     |
			// +----------------------------+----------------+-----------------+
			else if (v1 == 10 && v2 == 0)
			{
    			if (v3 >= 6000) return SQLSERVER_2008_SP4;
    			if (v3 >= 5861) return SQLSERVER_2008_SP3_CU17;
    			if (v3 >= 5852) return SQLSERVER_2008_SP3_CU16;
    			if (v3 >= 5850) return SQLSERVER_2008_SP3_CU15;
    			if (v3 >= 5848) return SQLSERVER_2008_SP3_CU14;
    			if (v3 >= 5846) return SQLSERVER_2008_SP3_CU13;
    			if (v3 >= 5844) return SQLSERVER_2008_SP3_CU12;
    			if (v3 >= 5840) return SQLSERVER_2008_SP3_CU11;
    			if (v3 >= 5835) return SQLSERVER_2008_SP3_CU10;
    			if (v3 >= 5829) return SQLSERVER_2008_SP3_CU9;
    			if (v3 >= 5828) return SQLSERVER_2008_SP3_CU8;
    			if (v3 >= 5794) return SQLSERVER_2008_SP3_CU7;
    			if (v3 >= 5788) return SQLSERVER_2008_SP3_CU6;
    			if (v3 >= 5785) return SQLSERVER_2008_SP3_CU5;
    			if (v3 >= 5775) return SQLSERVER_2008_SP3_CU4;
    			if (v3 >= 5770) return SQLSERVER_2008_SP3_CU3;
    			if (v3 >= 5768) return SQLSERVER_2008_SP3_CU2;
    			if (v3 >= 5766) return SQLSERVER_2008_SP3_CU1;
    			if (v3 >= 5500) return SQLSERVER_2008_SP3;
    			if (v3 >= 4332) return SQLSERVER_2008_SP2_CU10;
    			if (v3 >= 4330) return SQLSERVER_2008_SP2_CU9;
    			if (v3 >= 4326) return SQLSERVER_2008_SP2_CU8;
    			if (v3 >= 4323) return SQLSERVER_2008_SP2_CU7;
    			if (v3 >= 4321) return SQLSERVER_2008_SP2_CU6;
    			if (v3 >= 4316) return SQLSERVER_2008_SP2_CU5;
    			if (v3 >= 4285) return SQLSERVER_2008_SP2_CU4;
    			if (v3 >= 4279) return SQLSERVER_2008_SP2_CU3;
    			if (v3 >= 4272) return SQLSERVER_2008_SP2_CU2;
    			if (v3 >= 4266) return SQLSERVER_2008_SP2_CU1;
    			if (v3 >= 4000) return SQLSERVER_2008_SP2;
    			if (v3 >= 2850) return SQLSERVER_2008_SP1_CU16;
    			if (v3 >= 2847) return SQLSERVER_2008_SP1_CU15;
    			if (v3 >= 2821) return SQLSERVER_2008_SP1_CU14;
    			if (v3 >= 2816) return SQLSERVER_2008_SP1_CU13;
    			if (v3 >= 2808) return SQLSERVER_2008_SP1_CU12;
    			if (v3 >= 2804) return SQLSERVER_2008_SP1_CU11;
    			if (v3 >= 2799) return SQLSERVER_2008_SP1_CU10;
    			if (v3 >= 2789) return SQLSERVER_2008_SP1_CU9;
    			if (v3 >= 2775) return SQLSERVER_2008_SP1_CU8;
    			if (v3 >= 2766) return SQLSERVER_2008_SP1_CU7;
    			if (v3 >= 2757) return SQLSERVER_2008_SP1_CU6;
    			if (v3 >= 2746) return SQLSERVER_2008_SP1_CU5;
    			if (v3 >= 2734) return SQLSERVER_2008_SP1_CU4;
    			if (v3 >= 2723) return SQLSERVER_2008_SP1_CU3;
    			if (v3 >= 2714) return SQLSERVER_2008_SP1_CU2;
    			if (v3 >= 2710) return SQLSERVER_2008_SP1_CU1;
    			if (v3 >= 2531) return SQLSERVER_2008_SP1;
    			if (v3 >= 1835) return SQLSERVER_2008_CU10;
    			if (v3 >= 1828) return SQLSERVER_2008_CU9;
    			if (v3 >= 1823) return SQLSERVER_2008_CU8;
    			if (v3 >= 1818) return SQLSERVER_2008_CU7;
    			if (v3 >= 1812) return SQLSERVER_2008_CU6;
    			if (v3 >= 1806) return SQLSERVER_2008_CU5;
    			if (v3 >= 1798) return SQLSERVER_2008_CU4;
    			if (v3 >= 1787) return SQLSERVER_2008_CU3;
    			if (v3 >= 1779) return SQLSERVER_2008_CU2;
    			if (v3 >= 1763) return SQLSERVER_2008_CU1;
    			if (v3 >= 1600) return SQLSERVER_2008;
			}

			// #################################################################
			// ## SQL Server 2008 R2
			// #################################################################
			// +----------------------------+----------------+-----------------+
			// | Version                    | Build          | Release Date    |
			// +----------------------------+----------------+-----------------+
			// | Service Pack 3 =========== | 10.50.6000     | September 2014  |
			// | SP2 CU-13                  | 10.50.4319     | June 2014       |
			// | SP2 CU-12                  | 10.50.4305     | April 2014      |
			// | SP2 CU-11                  | 10.50.4302     | February 2014   |
			// | SP2 CU-10                  | 10.50.4297     | December 2013   |
			// | SP2 CU-9                   | 10.50.4295     | October 2013    |
			// | SP2 CU-8                   | 10.50.4290     | August 2013     |
			// | SP2 CU-7                   | 10.50.4286     | June 2013       |
			// | SP2 CU-6                   | 10.50.4279     | April 2013      |
			// | SP2 CU-5                   | 10.50.4276     | February 2013   |
			// | SP2 CU-4                   | 10.50.4270     | December 2012   |
			// | SP2 CU-3                   | 10.50.4266     | October 2012    |
			// | SP2 CU-2                   | 10.50.4263     | August 2012     |
			// | SP2 CU-1                   | 10.50.4260     | August 2012     |
			// | Service Pack 2 =========== | 10.50.4000     | July 2012       |
			// | SP1 CU-10                  | 10.50.2868     | December 2012   |
			// | SP1 CU-8                   | 10.50.2817     | August 2012     |
			// | SP1 CU-7                   | 10.50.2822     | June 2012       |
			// | SP1 CU-6                   | 10.50.2811     | April, 2012     |
			// | SP1 CU-5                   | 10.50.2806     | February 2012   |
			// | SP1 CU-4                   | 10.50.2796     | December 2011   |
			// | SP1 CU-3                   | 10.50.2789     | October 2011    |
			// | SP1 CU-2                   | 10.50.2772     | August 2011     |
			// | SP1 CU-1                   | 10.50.2769     | July 2011       |
			// | Service Pack 1 =========== | 10.50.2500     | July 2011       |
			// | RTM CU-14                  | 10.50.1817     | June 2012       |
			// | RTM CU-13                  | 10.50.1815     | April 2012      |
			// | RTM CU-12                  | 10.50.1810     | March 2012      |
			// | RTM CU-11                  | 10.50.1809     | February 2012   |
			// | RTM CU-10                  | 10.50.1807     | October 2011    |
			// | RTM CU-9                   | 10.50.1804     | August 2011     |
			// | RTM CU-8                   | 10.50.1797     | June 2011       |
			// | RTM CU-7                   | 10.50.1777     | April 2011      |
			// | RTM CU-6                   | 10.5.1765      | February 2011   |
			// | RTM CU-5                   | 10.5.1753      | December 2010   |
			// | RTM CU-4                   | 10.5.1746      | October 2010    |
			// | RTM CU-3                   | 10.5.1734      | August 2010     |
			// | RTM CU-2                   | 10.5.1720      | June 2010       |
			// | RTM CU-1                   | 10.5.1702      | May 2010        |
			// | RTM                        | 10.5.1600      | April 2010      |
			// +----------------------------+----------------+-----------------+
			else if ( v1 == 10 && ((v2 == 5) || (v2 == 50)) )
			{
    			if (v3 >= 6000) return SQLSERVER_2008R2_SP3;
    			if (v3 >= 4319) return SQLSERVER_2008R2_SP2_CU13;
    			if (v3 >= 4305) return SQLSERVER_2008R2_SP2_CU12;
    			if (v3 >= 4302) return SQLSERVER_2008R2_SP2_CU11;
    			if (v3 >= 4297) return SQLSERVER_2008R2_SP2_CU10;
    			if (v3 >= 4295) return SQLSERVER_2008R2_SP2_CU9;
    			if (v3 >= 4290) return SQLSERVER_2008R2_SP2_CU8;
    			if (v3 >= 4286) return SQLSERVER_2008R2_SP2_CU7;
    			if (v3 >= 4279) return SQLSERVER_2008R2_SP2_CU6;
    			if (v3 >= 4276) return SQLSERVER_2008R2_SP2_CU5;
    			if (v3 >= 4270) return SQLSERVER_2008R2_SP2_CU4;
    			if (v3 >= 4266) return SQLSERVER_2008R2_SP2_CU3;
    			if (v3 >= 4263) return SQLSERVER_2008R2_SP2_CU2;
    			if (v3 >= 4260) return SQLSERVER_2008R2_SP2_CU1;
    			if (v3 >= 4000) return SQLSERVER_2008R2_SP2;
    			if (v3 >= 2868) return SQLSERVER_2008R2_SP1_CU10;
    			// NOTE: SP1_CU9 but nothing in the above list
    			if (v3 >= 2822) return SQLSERVER_2008R2_SP1_CU8; // strange according to the above list CU7 has a higher number than CU8, I guess it's a typo
    			if (v3 >= 2817) return SQLSERVER_2008R2_SP1_CU7; // strange according to the above list CU7 has a higher number than CU8, I guess it's a typo
    			if (v3 >= 2811) return SQLSERVER_2008R2_SP1_CU6;
    			if (v3 >= 2806) return SQLSERVER_2008R2_SP1_CU5;
    			if (v3 >= 2796) return SQLSERVER_2008R2_SP1_CU4;
    			if (v3 >= 2789) return SQLSERVER_2008R2_SP1_CU3;
    			if (v3 >= 2772) return SQLSERVER_2008R2_SP1_CU2;
    			if (v3 >= 2769) return SQLSERVER_2008R2_SP1_CU1;
    			if (v3 >= 2500) return SQLSERVER_2008R2_SP1;
    			if (v3 >= 1817) return SQLSERVER_2008R2_CU14;
    			if (v3 >= 1815) return SQLSERVER_2008R2_CU13;
    			if (v3 >= 1810) return SQLSERVER_2008R2_CU12;
    			if (v3 >= 1809) return SQLSERVER_2008R2_CU11;
    			if (v3 >= 1807) return SQLSERVER_2008R2_CU10;
    			if (v3 >= 1804) return SQLSERVER_2008R2_CU9;
    			if (v3 >= 1797) return SQLSERVER_2008R2_CU8;
    			if (v3 >= 1777) return SQLSERVER_2008R2_CU7;
    			if (v3 >= 1765) return SQLSERVER_2008R2_CU6;
    			if (v3 >= 1753) return SQLSERVER_2008R2_CU5;
    			if (v3 >= 1746) return SQLSERVER_2008R2_CU4;
    			if (v3 >= 1734) return SQLSERVER_2008R2_CU3;
    			if (v3 >= 1720) return SQLSERVER_2008R2_CU2;
    			if (v3 >= 1702) return SQLSERVER_2008R2_CU1;
    			if (v3 >= 1600) return SQLSERVER_2008R2;
			}

			// #################################################################
			// ## SQL Server 2012
			// #################################################################
			// +----------------------------+----------------+-----------------+
			// | Version                    | Build          | Release Date    |
			// +----------------------------+----------------+-----------------+
			// | Service Pack 3 =========== | 11.0.6020.0    | November 2015   |
			// | SP2 CU-9                   | 11.0.5641.0    | November 2015   |
			// | SP2 CU-8                   | 11.0.5634.0    | September 2015  |
			// | SP2 CU-7                   | 11.0.5623.0    | July 2015       |
			// | SP2 CU-6                   | 11.0.5592.0    | May 2015        |
			// | SP2 CU-5                   | 11.0.5582.0    | March 2015      |
			// | SP2 CU-4                   | 11.0.5569.0    | January 2015    |
			// | SP2 CU-3                   | 11.0.5556.0    | November 2014   |
			// | SP2 CU-2                   | 11.0.5548.0    | September 2014  |
			// | SP2 CU-1                   | 11.0.5532.0    | July 2014       |
			// | Service Pack 2 =========== | 11.0.5058.0    | June 2014       |
			// | SP1 CU-13                  | 11.0.3482.0    | November 2014   |
			// | SP1 CU-12                  | 11.0.3470.0    | September 2014  |
			// | SP1 CU-11                  | 11.0.3449.0    | July 2014       |
			// | SP1 CU-10                  | 11.0.3431.0    | May 2014        |
			// | SP1 CU-9                   | 11.0.3412.0    | March 2014      |
			// | SP1 CU-8                   | 11.0.3401.0    | January 2014    |
			// | SP1 CU-7                   | 11.0.3393.0    | November 2013   |
			// | SP1 CU-6                   | 11.0.3381.0    | September 2013  |
			// | SP1 CU-5                   | 11.0.3373.0    | July 2013       |
			// | SP1 CU-4                   | 11.0.3368.0    | May 2013        |
			// | SP1 CU-3                   | 11.0.3349.0    | March 2013      |
			// | SP1 CU-2                   | 11.0.3339.0    | January 2013    |
			// | SP1 CU-1                   | 11.0.3321.0    | November 2012   |
			// | Service Pack 1 =========== | 11.0.3000.0    | November 2012   |
			// | CU-5                       | 11.0.2395.0    | December 2012   |
			// | CU-4                       | 11.0.2383.0    | September 2012  |
			// | CU-3                       | 11.0.2332.0    | August 2012     |
			// | CU-2                       | 11.0.2325.0    | June 2012       |
			// | CU-1                       | 11.0.2316.0    | April 2012      |
			// | RTM                        | 11.0.2100.60   | March 2011      |
			// +----------------------------+----------------+-----------------+
			else if (v1 == 11 && v2 == 0)
			{
    			if (v3 >= 6020) return SQLSERVER_2012_SP3;
    			if (v3 >= 5641) return SQLSERVER_2012_SP2_CU9;
    			if (v3 >= 5634) return SQLSERVER_2012_SP2_CU8;
    			if (v3 >= 5623) return SQLSERVER_2012_SP2_CU7;
    			if (v3 >= 5592) return SQLSERVER_2012_SP2_CU6;
    			if (v3 >= 5582) return SQLSERVER_2012_SP2_CU5;
    			if (v3 >= 5569) return SQLSERVER_2012_SP2_CU4;
    			if (v3 >= 5556) return SQLSERVER_2012_SP2_CU3;
    			if (v3 >= 5548) return SQLSERVER_2012_SP2_CU2;
    			if (v3 >= 5532) return SQLSERVER_2012_SP2_CU1;
    			if (v3 >= 5058) return SQLSERVER_2012_SP2;
    			if (v3 >= 3482) return SQLSERVER_2012_SP1_CU13;
    			if (v3 >= 3470) return SQLSERVER_2012_SP1_CU12;
    			if (v3 >= 3449) return SQLSERVER_2012_SP1_CU11;
    			if (v3 >= 3431) return SQLSERVER_2012_SP1_CU10;
    			if (v3 >= 3412) return SQLSERVER_2012_SP1_CU9;
    			if (v3 >= 3401) return SQLSERVER_2012_SP1_CU8;
    			if (v3 >= 3393) return SQLSERVER_2012_SP1_CU7;
    			if (v3 >= 3381) return SQLSERVER_2012_SP1_CU6;
    			if (v3 >= 3373) return SQLSERVER_2012_SP1_CU5;
    			if (v3 >= 3368) return SQLSERVER_2012_SP1_CU4;
    			if (v3 >= 3349) return SQLSERVER_2012_SP1_CU3;
    			if (v3 >= 3339) return SQLSERVER_2012_SP1_CU2;
    			if (v3 >= 3321) return SQLSERVER_2012_SP1_CU1;
    			if (v3 >= 3000) return SQLSERVER_2012_SP1;
    			if (v3 >= 2395) return SQLSERVER_2012_CU5;
    			if (v3 >= 2383) return SQLSERVER_2012_CU4;
    			if (v3 >= 2332) return SQLSERVER_2012_CU3;
    			if (v3 >= 2325) return SQLSERVER_2012_CU2;
    			if (v3 >= 2316) return SQLSERVER_2012_CU1;
    			if (v3 >= 2100 && v3 >= 60) return SQLSERVER_2012;
			}
			
			// #################################################################
			// ## SQL Server 2014
			// #################################################################
			// +----------------------------+----------------+-----------------+
			// | Version                    | Build          | Release Date    |
			// +----------------------------+----------------+-----------------+
			// | Version                    | Build          | Release Date    |
			// | SP1 CU-3                   | 12.0.4427.24   | October 2015    |
			// | SP1 CU-2                   | 12.0.4422.0    | August 2015     |
			// | SP1 CU-1                   | 12.0.4416.0    | June 2015       |
			// | Service Pack 1 =========== | 12.0.4100.0    | May 2015        |
			// | CU-8                       | 12.0.2546.0    | June 2015       |
			// | CU-7                       | 12.0.2495.0    | April 2015      |
			// | CU-6                       | 12.0.2480.0    | February 2015   |
			// | CU-5                       | 12.0.2456.0    | December 2014   |
			// | CU-4                       | 12.0.2430.0    | October 2014    |
			// | CU-3                       | 12.0.2402.0    | August 2014     |
			// | CU-2                       | 12.0.2370.0    | June 2014       |
			// | CU-1                       | 12.0.2342.0    | April 2014      |
			// | RTM                        | 12.0.2000.0    | April 2014      |
			// +----------------------------+----------------+-----------------+
			else if (v1 == 12 && v2 == 0)
			{
    			if (v3 >= 4427 && v4 >= 24) return SQLSERVER_2014_SP1_CU3;
    			if (v3 >= 4422) return SQLSERVER_2014_SP1_CU2;
    			if (v3 >= 4416) return SQLSERVER_2014_SP1_CU1;
    			if (v3 >= 4100) return SQLSERVER_2014_SP1;
    			if (v3 >= 2546) return SQLSERVER_2014_CU8;
    			if (v3 >= 2495) return SQLSERVER_2014_CU7;
    			if (v3 >= 2480) return SQLSERVER_2014_CU6;
    			if (v3 >= 2456) return SQLSERVER_2014_CU5;
    			if (v3 >= 2430) return SQLSERVER_2014_CU4;
    			if (v3 >= 2402) return SQLSERVER_2014_CU3;
    			if (v3 >= 2370) return SQLSERVER_2014_CU2;
    			if (v3 >= 2342) return SQLSERVER_2014_CU1;
    			if (v3 >= 2000) return SQLSERVER_2014;
			}

			// #################################################################
			// ## SQL Server 2016
			// ## I don't know the version string for those, so I'm just guessing it will be 13.0
			// #################################################################
			// +----------------------------+----------------+-----------------+
			// | Version                    | Build          | Release Date    |
			// +----------------------------+----------------+-----------------+
			// | no-info                    | no-info        | no-info         |
			// +----------------------------+----------------+-----------------+
			else if (v1 == 13 && v2 == 0)
			{
    			return SQLSERVER_2016;
			}

			// #################################################################
			// ## Just guessing here
			// ## If it's above 13, then we will just have to say SQL-Server 2016
			// #################################################################
			else if (v1 >= 13)
			{
    			return SQLSERVER_2016;
			}
		}
		return intVersion;
	}
}
