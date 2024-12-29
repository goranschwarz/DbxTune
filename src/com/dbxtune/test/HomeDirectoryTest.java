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

import java.io.File;
import java.io.IOException;

public class HomeDirectoryTest
{
	private static final String USER_HOME = System.getProperty("user.home");
	private static final String DBXTUNE_HOME_STR = USER_HOME+"/.dbxtune";

	public static void main(String[] args)
	{
		File dbxtuneHomeDir = new File(DBXTUNE_HOME_STR);

		
		try
		{
			System.out.println("dbxtuneHomeDir='"+dbxtuneHomeDir.getCanonicalPath()+"'.");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		if (dbxtuneHomeDir.exists())
		{
			System.out.println("dir '"+DBXTUNE_HOME_STR+"' existed.");
		}
	}
}
