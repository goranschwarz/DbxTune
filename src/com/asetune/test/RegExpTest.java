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
package com.asetune.test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegExpTest
{
	public static void main(String[] args)
	{
//		Regex DDL = new Regex(@"(?<=\b(create|alter|drop)\s+(procedure|proc|table|trigger|view|function)\b\s\[dbo\].)\[.*?\]", RegexOptions.IgnoreCase);
//		Match match = DDL.Match(inputScript);
//		while(match.Success)
//		{
//			textBox2.Text += "\r\n" + match.Groups[1].Value + " - " + match.Groups[2].Value + " - " +  match.Value;
//			match = match.NextMatch();
//		}
//		textBox3.Text = "DONE";

		String test1 = 
			"create proc nisse()\n" +
			"as \n" +
			"begin \n" +
			"end";

		String test2 = 
			"" +
			"\n" +
			"create procedure nisse()\n" +
			"as \n" +
			"begin \n" +
			"end";
		
		String test3 = 
			"" +
			"\n" +
			"create function nisse()\n" +
			"as \n" +
			"begin \n" +
			"end";
		
		String test4 = 
			"" +
			"\n" +
			"create table nisse()\n" +
			"as \n" +
			"begin \n" +
			"end";
		
		
//		String regex = "(?<=\\b(create|alter)\\s+(procedure|proc|trigger|view|function)\\b\\s\\[dbo\\].)\\[.*?\\]";
		String regex = "(create|alter)\\s+(procedure|proc|trigger|view|function)";

//		Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.UNIX_LINES);
		Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(test1);
//		return m.matches();
//		m.find()

		System.out.println("matches: TEST 1: "+p.matcher(test1).matches());
		System.out.println("matches: TEST 2: "+p.matcher(test2).matches());
		System.out.println("matches: TEST 3: "+p.matcher(test3).matches());
		System.out.println("matches: TEST 4: "+p.matcher(test4).matches());

		System.out.println("find: TEST 1: "+p.matcher(test1).find());
		System.out.println("find: TEST 2: "+p.matcher(test2).find());
		System.out.println("find: TEST 3: "+p.matcher(test3).find());
		System.out.println("find: TEST 4: "+p.matcher(test4).find());
	}
}
