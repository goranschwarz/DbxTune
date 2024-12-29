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
package com.dbxtune.utils;

/**
 * This class detects the Java-Version
 */
public class JavaBitness
{
	/**
	 * Returns the Version of Java.
	 */
	public static int getBitness()
	{
		String bitnessStr = System.getProperty("sun.arch.data.model");
		if (bitnessStr == null)
			return 0;

		try
		{
			return Integer.parseInt(bitnessStr);
		}
		catch (Throwable e)
		{
			// TODO: handle exception
		}

		return 0;
	}


	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		System.exit(getBitness());
	}
}
