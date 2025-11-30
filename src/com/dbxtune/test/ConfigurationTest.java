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

import com.dbxtune.utils.Configuration;

public class ConfigurationTest
{

	public static void main(String[] args)
	{
		// Set Log4j Log Level
//		Configurator.setRootLevel(Level.TRACE);

		test1();
		test2();
	}
	
	private static void test1()
	{
		Configuration conf = new Configuration();
		conf.setProperty("dummy", "xxx");
		conf.save();

		String dummy = conf.getProperty("dummy");
		System.out.println("dummy='"+dummy+"'");
		if ( ! "xxx".equals(dummy))
			System.out.println("test 1: ----------------------- FAILED -------------------------------");
	}


	private static void test2()
	{
		Configuration conf = new Configuration();
		conf.setProperty("dummy", "xxx");
		conf.save();

		String dummy = conf.getProperty("dummy");
		System.out.println("dummy='"+dummy+"'");
		if ( ! "xxx".equals(dummy))
			System.out.println("test 1: ----------------------- FAILED -------------------------------");
	}
}
