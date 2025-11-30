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
package com.dbxtune.test;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class Exodus
{
	private static String oracleDatePatternToJava(String pattern)
	{
		pattern = pattern.replaceAll("YY",            "yy");
		pattern = pattern.replaceAll("DD",            "dd");
		pattern = pattern.replaceAll("HH24|hh24",     "HH");
		pattern = pattern.replaceAll("HH?!24|hh?!24", "KK");
		pattern = pattern.replaceAll("MON|mon",       "MMM");
		pattern = pattern.replaceAll("MI|mi",         "mm");
		pattern = pattern.replaceAll("SS|ss",         "ss");
		pattern = pattern.replaceAll("AM|PM",         "aa");

		return pattern;
	}
//	public static String TO_CHAR(String date, String pattern)
	public static String TO_CHAR(java.util.Date dt, String pattern)
	{
		pattern = oracleDatePatternToJava(pattern);
//		java.util.Date dt;
//		
//		if(date.length() > 10)
//			dt = java.sql.Timestamp.valueOf(date);
//		else
//			dt = java.sql.Date.valueOf(date);

		SimpleDateFormat sm = new SimpleDateFormat(pattern);
		return sm.format(dt);
	}
	
	public static String TO_CHAR(BigDecimal number, String pattern)
	{
		pattern = oracleDatePatternToJava(pattern);
	    return new java.text.DecimalFormat(pattern).format(number);
	}

	public static java.util.Date TO_DATE(String date, String pattern)
	{
		pattern = oracleDatePatternToJava(pattern);
		SimpleDateFormat sm = new SimpleDateFormat(pattern);
		try
		{
			return sm.parse(date);
		}
		catch (ParseException e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public static void UTL_FILE()
	{
		/*
		FOPEN
		FCLOSE
		FFLUSH
		FGETATTR
		FOPEN
		FRENAME
		PUT
		PUT_LINE
		 */
	}

	public static void DBMS_LOB()
	{
		/*
		WRITEAPPEND
		GETLENGTH
		READ
		CREATETEMPORARY
		FREETEMPORARY
		 */
	}
	
	/*
	LAST_DAY			http://docs.oracle.com/cd/B19306_01/server.102/b14200/functions072.htm				
						LAST_DAY returns the date of the last day of the month that contains date. The return type is always DATE, regardless of the datatype of date.

	LEAST				http://docs.oracle.com/cd/B19306_01/server.102/b14200/functions075.htm#sthref1523
						LEAST returns the least of the list of exprs. All exprs after the first are implicitly converted to the datatype of the first expr before the comparison. Oracle Database compares the exprs using nonpadded comparison semantics. If the value returned by this function is character data, then its datatype is always VARCHAR2.
	
	LPAD				http://docs.oracle.com/cd/B19306_01/server.102/b14200/functions082.htm#sthref1575	
						LPAD returns expr1, left-padded to length n characters with the sequence of characters in expr2. This function is useful for formatting the output of a query.

	RPAD				http://docs.oracle.com/cd/B19306_01/server.102/b14200/functions140.htm#sthref2002
						RPAD returns expr1, right-padded to length n characters with expr2, replicated as many times as necessary. This function is useful for formatting the output of a query.
						
	MEDIAN				http://docs.oracle.com/cd/B19306_01/server.102/b14200/functions086.htm#sthref1601
						MEDIAN is an inverse distribution function that assumes a continuous distribution model. It takes a numeric or datetime value and returns the middle value or an interpolated value that would be the middle value once the values are sorted. Nulls are ignored in the calculation.
						
	MONTHS_BETWEEN		http://docs.oracle.com/cd/B19306_01/server.102/b14200/functions089.htm#sthref1622
						MONTHS_BETWEEN returns number of months between dates date1 and date2. If date1 is later than date2, then the result is positive. If date1 is earlier than date2, then the result is negative. If date1 and date2 are either the same days of the month or both last days of months, then the result is always an integer. Otherwise Oracle Database calculates the fractional portion of the result based on a 31-day month and considers the difference in time components date1 and date2.
						
	NEW_TIME			http://docs.oracle.com/cd/B19306_01/server.102/b14200/functions092.htm#sthref1641
						NEW_TIME returns the date and time in time zone timezone2 when date and time in time zone timezone1 are date. Before using this function, you must set the NLS_DATE_FORMAT parameter to display 24-hour time. The return type is always DATE, regardless of the datatype of date.

	NEXT_DAY			http://docs.oracle.com/cd/B19306_01/server.102/b14200/functions093.htm#sthref1647
						NEXT_DAY returns the date of the first weekday named by char that is later than the date date. The return type is always DATE, regardless of the datatype of date. The argument char must be a day of the week in the date language of your session, either the full name or the abbreviation. The minimum number of letters required is the number of letters in the abbreviated version. Any characters immediately following the valid abbreviation are ignored. The return value has the same hours, minutes, and seconds component as the argument date.

	 */
}


/*
create function to_char
(
	p1 datetime,
	p2 varchar(30)
)
returns varchar(255)
language java
parameter style java
external name 'dbxtune.com.test.Exodus.to_char(java.sql.Timestamp, java.lang.String)'
go
*/



//------------------------------------------------
// -- H2 function: TO_CHAR
//------------------------------------------------
//drop alias if exists TO_CHAR;
//create alias TO_CHAR as $$
//import java.text.SimpleDateFormat;
//import java.util.Date;
//@CODE
//String toChar(String date, String pattern) throws Exception {
//pattern = pattern.replaceAll("YY","yy");
//pattern = pattern.replaceAll("DD","dd");
//pattern = pattern.replaceAll("HH24|hh24","HH");
//pattern = pattern.replaceAll("HH?!24|hh?!24","KK");
//pattern = pattern.replaceAll("MON|mon","MMM");
//pattern = pattern.replaceAll("MI|mi","mm");
//pattern = pattern.replaceAll("SS|ss","ss");
//pattern = pattern.replaceAll("AM|PM","aa");
//SimpleDateFormat sm = new SimpleDateFormat(pattern);
//java.util.Date dt;
//  if(date.length() > 10)dt = java.sql.Timestamp.valueOf(date);
//else
// dt = java.sql.Date.valueOf(date);
//return sm.format(dt);
//
//}
//$$
//create table test(bday timestamp);
//insert into test values(today);
//select TO_CHAR(bday,'DD/MM/YYYY HH24:MI:ss') from test;
//
//create table test2(bday DATE);
//insert into test2 values(today);
//select TO_CHAR(bday,'DD/MM/YYYY HH24:MI:ss') from test2;



//------------------------------------------------
//-- H2 function: TO_CHAR
//------------------------------------------------
//-- TO_CHAR
//drop alias if exists TO_CHAR;
//create alias TO_CHAR as $$
//String toChar(BigDecimal x, String pattern) throws Exception {
//    return new java.text.DecimalFormat(pattern).format(x);
//}
//$$;
//call TO_CHAR(123456789.12, '###,###,###,###.##');
