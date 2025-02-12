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
package com.dbxtune.test;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

public class OpenSslAesUtil
{
	/**
	 * USAGE: java -cp ./0/lib/dbxtune.jar:./0/lib/bcprov-jdk15on-157.jar:./0/lib/log4j-1.2.17.jar:./0/lib/commons-codec-1.10.jar com.dbxtune.test.OpenSslAesUtil dbxtune mssql prod-a1-mssql
	 * @param args
	 */
	public static void main(String[] args)
	{
		// Set Log4j Log Level
		Configurator.setRootLevel(Level.TRACE);

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
			String passwd = com.dbxtune.utils.OpenSslAesUtil.readPasswdFromFile(user, server, filename, passphrase);
			System.out.println("<<-- Password '" + passwd + "'.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		
	}
}
