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


public class HtmlParserTest
{
	public static void main(String[] args)
	{
		String urlStr = "http://google.com";
		if (args.length >= 1)
		{
			urlStr = args[0];
			if ( ! urlStr.startsWith("http://") )
				urlStr = "http://" + urlStr;
		}
			
		System.out.println("Usage: progname url");
		System.out.println("URL: "+urlStr);

System.out.println("DOESN'T WORK, NEED TO UNCOMMENT");

//		Document doc;
//		try
//		{
//			// need http protocol
//			doc = Jsoup.connect(urlStr).get();
//
//			// get page title
//			String title = doc.title();
//			System.out.println("title : " + title);
//
//			// get all links
//			Elements links = doc.select("a[href]");
//			for (Element link : links)
//			{
//
//				// get the value from href attribute
//				System.out.println("\nlink : " + link.attr("href"));
//				System.out.println("text : " + link.text());
//
//			}
//
//		}
//		catch (IOException e)
//		{
//			e.printStackTrace();
//		}
	}
}
