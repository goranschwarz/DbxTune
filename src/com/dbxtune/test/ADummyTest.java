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

import com.dbxtune.utils.H2UrlHelper;

public class ADummyTest
{

	public static void main(String[] args)
	{
		String colName       = "123:4";
		String colModifier   = null;
		int    colNameSepPos = colName.indexOf(':');
		if (colNameSepPos > 0)
		{
			colModifier = colName.substring(colNameSepPos + 1);
			colName     = colName.substring(0, colNameSepPos);
		}
		System.out.println("colName    ='"+colName+"'.");
		System.out.println("colModifier='"+colModifier+"'.");


		//xxx: get/set last file choosen...action in some way...
		testUrl("jdbc:h2:file:C:/projects/dbxtune/data/xxx;IFEXISTS=TRUE");
		testUrl("jdbc:h2:~/test;IFEXISTS=TRUE");
		testUrl("jdbc:h2:file:/data/sample;IFEXISTS=TRUE");
		testUrl("jdbc:h2:file:C:/data/sample;IFEXISTS=TRUE");
		testUrl("jdbc:h2:file:");
		testUrl("jdbc:h2:file:[<path>]<dbname>");
		testUrl("jdbc:h2:tcp://<host>[:<port>]/<dbname>");
		testUrl("jdbc:h2:ssl://<host>[:<port>]/<dbname>");
	}
	public static void testUrl(String url)
	{
		System.out.println("-------------------------------------------------------------");
		System.out.println("INPUT url='"+url+"'.");

		H2UrlHelper h = new H2UrlHelper(url);
		System.out.println("h.getRawFileString() = '"+h.getRawFileString()        +"'.");
		System.out.println("h.getFile()          = '"+h.getFile()                 +"'.");
		System.out.println("h.getDir()           = '"+h.getDir()                  +"'.");
		System.out.println("h.getDir(def)        = '"+h.getDir("c:/tmp") +"'.");

//		ConnectionInfo ci = new ConnectionInfo(url, new Properties());
//		System.out.println("getUrl         = '"+ci.getURL()         +"'.");
//		System.out.println("getOriginalURL = '"+ci.getOriginalURL() +"'.");
//		System.out.println("getUserName    = '"+ci.getUserName()    +"'.");
	}


//	/**
//	 * @param args
//	 */
//	public static void main(String[] args)
//	{
//		String xxx;
//		xxx = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()); System.out.println("XXX='"+xxx+"'.");
//		xxx = new SimpleDateFormat("yyyy-MM-dd HH:m:s").format(new Date()); System.out.println("XXX='"+xxx+"'.");
//		xxx = new SimpleDateFormat("yyyy-MM-dd HH:m").format(new Date()); System.out.println("XXX='"+xxx+"'.");
//		System.exit(0);
//
//		// Extract Environment variables
//		// search for ${ENV_NAME}
//		String val = "abc ${DATE:format=yyyyMMdd.HHmm; roll=true} xxx ${SERVERNAME}:${XXX}";
//		System.out.println("BEFORE: val='"+val+"'.");
//
//		Pattern compiledRegex = Pattern.compile("\\$\\{.*\\}");
//		while( compiledRegex.matcher(val).find() )
//		{
//			String varVal      = null;
//			String varStr      = val.substring( val.indexOf("${")+2, val.indexOf("}") );
//			String varName     = varStr;
//			Configuration varConf = null;
//			if (varStr.indexOf(':') >= 0)
//			{
//				int firstColon = varStr.indexOf(':');
//				varName = varStr.substring(0, firstColon);
//				String varModifyer = varStr.substring(firstColon + 1);
//				
//				try { varConf = Configuration.parse(varModifyer, ";"); }
//				catch (ParseException e) {}
//			}
//
//			System.out.println("varName='"+varName+"', varModifyer='"+varConf+"'.");
//			
//			if ( "DATE".equals(varName) )
//			{
//				String defaultFormat = "yyyy-MM-dd";
//				String dateFormat = varConf == null ? defaultFormat : varConf.getProperty("format", defaultFormat);
//				varVal = new SimpleDateFormat(dateFormat).format(new Date());
//			}
//			else if ( "SERVERNAME".equals(varName) )
//			{
//				varVal = "@@srvName";
//			}
//			else if ( "HOSTNAME".equals(varName) )
//			{
//				varVal = "hostname()";
//			}
//			else
//			{
//				System.out.println("WARNING: Unknown variable '"+varName+"', simply removing it from the output.");
//				varVal = "";
//				//varVal = "$"+varStr+"";
//			}
//
//			// NOW substityte the ENVVARIABLE with a real value...
//			val = val.replace("${"+varStr+"}", varVal);
//		}
//		System.out.println("AFTER: val='"+val+"'.");
//	}

}
