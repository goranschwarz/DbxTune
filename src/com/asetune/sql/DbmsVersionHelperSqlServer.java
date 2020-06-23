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
package com.asetune.sql;

public class DbmsVersionHelperSqlServer
implements IDbmsVersionHelper
{
	@Override
	public String versionNumToStr(long version, int major, int minor, int maintenance, int sp, int cu)
	{
		// https://sqlserverbuilds.blogspot.com/

		String verStr = "";

		verStr = major + "";
		
		if (minor > 0)
			verStr += " R"+minor;  // SQL Server 2008 R2
		
		if (sp > 0)
			verStr += " SP"+sp;
		
		if (cu > 0)
			verStr += " CU"+cu;
		
		return verStr;
		
//		if (major == 7)
//		{
//			verStr = "7.0";
//		}
//		else if (major == 8)
//		{
//			verStr = "2000";
//			if (f4 >= 384 && f4 < 532)  verStr += " SP1";
//			if (f4 >= 532 && f4 < 760)  verStr += " SP2";
//			if (f4 >= 760 && f4 < 2039) verStr += " SP3";
//			if (f4 >= 2039)             verStr += " SP4";
//		}
//		else if (major == 9)
//		{
//			verStr = "2005";
//			if (f4 >= 2047 && f4 < 3042) verStr += " SP1";
//			if (f4 >= 3042 && f4 < 4035) verStr += " SP2";
//			if (f4 >= 4035 && f4 < 2039) verStr += " SP3";
//			if (f4 >= 5000)              verStr += " SP4";
//		}
//		else if (major == 10)
//		{
//			if (minor == 0)
//			{
//				verStr = "2008";
//
//				if (f4 >= 2531 && f4 < 4000) verStr += " SP1";
//				if (f4 >= 4000 && f4 < 5500) verStr += " SP2";
//				if (f4 >= 5500 && f4 < 6000) verStr += " SP3";
//				if (f4 >= 6000)              verStr += " SP4";
//			}
//			else
//			{
//				verStr = "2008 R2";
//				
//				if (f4 >= 2500 && f4 < 4000) verStr += " SP1";
//				if (f4 >= 4000 && f4 < 6000) verStr += " SP2";
//				if (f4 >= 6000)              verStr += " SP3";
//			}
//		}
//		else if (major == 11)
//		{
//			verStr = "2012";
//
//			if (f4 >= 3000 && f4 < 5058) verStr += " SP1";
//			if (f4 >= 5058 && f4 < 6020) verStr += " SP2";
//			if (f4 >= 6020 && f4 < 7001) verStr += " SP3";
//			if (f4 >= 7001)              verStr += " SP4";
//		}
//		else if (major == 12)
//		{
//			verStr = "2014";
//
//			if (f4 >= 4100 && f4 < 5000) verStr += " SP1";
//			if (f4 >= 5000 && f4 < 6024) verStr += " SP2";
//			if (f4 >= 6024)              verStr += " SP3";
//		}
//		else if (major == 13)
//		{
//			verStr = "2016";
//
//			if (f4 >= 4101 && f4 < 5026) verStr += " SP1";
//			if (f4 >= 5026)              verStr += " SP2";
//		}
//		else if (major == 14)
//		{
//			verStr = "2017";
//		}
//		else if (major == 15)
//		{
//			verStr = "2019";
//		}
//		else
//		{
//			verStr = "20??";
//		}
//		
//		return verStr;
	}
}
