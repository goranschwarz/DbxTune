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
package com.asetune.test;

import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;

public class OpenSslAesUtil
{
	/**
	 * USAGE: java -cp ./0/lib/asetune.jar:./0/lib/bcprov-jdk15on-157.jar:./0/lib/log4j-1.2.17.jar:./0/lib/commons-codec-1.10.jar com.asetune.test.OpenSslAesUtil dbxtune mssql prod-a1-mssql
	 * @param args
	 */
	public static void main(String[] args)
	{
		Properties log4jProps = new Properties();
//		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		log4jProps.setProperty("log4j.rootLogger", "TRACE, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

		System.out.println("Usage: user passphrase [server] [filename]");
		String user       = null;
		String passphrase = null;
		String server     = null;
		String filename   = null;
		
		if (args.length > 0) user       = args[0];
		if (args.length > 1) passphrase = args[1];
		if (args.length > 2) server     = args[2];
		if (args.length > 3) filename   = args[3];

		System.out.println("user       = '"+user+"'");
		System.out.println("passphrase = '"+passphrase+"'");
		System.out.println("server     = '"+server+"'");
		System.out.println("filename   = '"+filename+"'");
		
		if (user == null && passphrase == null)
			System.exit(1);


		try
		{
			String passwd = com.asetune.utils.OpenSslAesUtil.readPasswdFromFile(user, server, filename, passphrase);
			System.out.println("<<-- Password '" + passwd + "'.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		
	}
}
