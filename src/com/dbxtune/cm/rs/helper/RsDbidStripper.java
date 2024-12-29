/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.cm.rs.helper;

import org.apache.commons.lang3.StringUtils;

import com.dbxtune.utils.StringUtil;

public class RsDbidStripper
{

	/**
	 * Strips off RepServer DBID in strings<br>
	 * <ul>
	 *    <li>If first word is a number, simply remove it from the output</li>
	 *    <li>If first word is a queue-number, simply remove it from the output, and append the queue type. ':1' to ' (in-q), ':0' to ' (out-q)' </li>
	 * </ul>
	 * 
	 * Examples:
	 * <table>
	 *    <tr> <th>input</th> <th>returns</th> </tr>
	 *    <tr> <td><i>empty or null</i></td>                  <td><i>input str</i></td> </tr>
	 *    <tr> <td>101 PROD_REP_RSSD.PROD_REP_RSSD</td>       <td>PROD_REP_RSSD.PROD_REP_RSSD   </td> </tr>
	 *    <tr> <td>124 PROD_A1_ASE.gorans         </td>       <td>PROD_A1_ASE.gorans            </td> </tr>
	 *    
	 *    <tr> <td>123 LDS.gorans                 </td>       <td>LDS.gorans                    </td> </tr>
	 *    
	 *    <tr> <td>123:1  DIST LDS.gorans         </td>       <td>DIST LDS.goran (in-q)         </td> </tr>
	 *    <tr> <td>124 PROD_A1_ASE.gorans         </td>       <td>PROD_A1_ASE.gorans            </td> </tr>
	 *    <tr> <td>125 PROD_B1_ASE.gorans         </td>       <td>PROD_B1_ASE.gorans            </td> </tr>
	 *    
	 *    <tr> <td>123:0 LDS.gorans               </td>       <td>LDS.gorans (out-q)            </td> </tr>
	 *    <tr> <td>123:1 LDS.gorans               </td>       <td>LDS.gorans (in-q)             </td> </tr>
	 * </table>
	 * 
	 * @param str
	 * @return see above
	 */
	public static String stripDbid(String str)
	{
		if (StringUtil.isNullOrBlank(str))
			return str;

		int firstSpace = str.indexOf(' ');
		if (firstSpace >= 0)
		{
			String firstWord = str.substring(0, firstSpace).trim();
			String restWord  = str.substring(firstSpace + 1).trim();
			String queueType = "";
			
			// is it a "in" or "out" queue... 1=IN, 0=OUT
			if (firstWord.indexOf(':') >= 0)
			{
				if (firstWord.endsWith(":1")) { queueType = " (in-q)";  firstWord = firstWord.substring(0, firstWord.length()-2); }
				if (firstWord.endsWith(":0")) { queueType = " (out-q)"; firstWord = firstWord.substring(0, firstWord.length()-2); }
			}

			// If first word is a number (or it's a number:qType), simply remove it
			if (StringUtils.isNumeric(firstWord))
			{
				return restWord + queueType;
			}
		}

		// if we got here we simply return the input string
		return str;
	}

	private static void test(String in, String exp)
	{
		String str = stripDbid(in);
		if (str.equals(exp))
		{
			System.out.println("SUCCESS: input='" + in + "', output='" + str + "', expectedResult='" + exp + "'.");
		}
		else
		{
			System.out.println(">> FAIL: input='" + in + "', output='" + str + "', expectedResult='" + exp + "'.");
		}
	}
	
	public static void main(String[] args)
	{
		test("",                                "");
		test("101 PROD_REP_RSSD.PROD_REP_RSSD", "PROD_REP_RSSD.PROD_REP_RSSD");
		test("124 PROD_A1_ASE.gorans"         , "PROD_A1_ASE.gorans");
		test("123 LDS.gorans"                 , "LDS.gorans");
		test("123:1  DIST LDS.gorans"         , "DIST LDS.gorans (in-q)");
		test("124 PROD_A1_ASE.gorans"         , "PROD_A1_ASE.gorans");
		test("125 PROD_B1_ASE.gorans"         , "PROD_B1_ASE.gorans");
		test("123:0 LDS.gorans"               , "LDS.gorans (out-q)");
		test("123:1 LDS.gorans"               , "LDS.gorans (in-q)");

		// Other small test cases
		test("aaa"               , "aaa");
		test("aaa bbb"           , "aaa bbb");
		test("aaa bbb ccc "      , "aaa bbb ccc ");
	}
}
