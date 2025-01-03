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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class VersionShortTest
{

	@Test
	public void test()
	{
		// Test "raw" integers and generated integers
		assertEquals(100101,  VersionShort.toInt(10, 1, 1));
		assertEquals( 10000,  VersionShort.toInt( 1, 0, 0));

		// Test generated-integers  and  parsed version str
		assertEquals(VersionShort.toInt(10, 1, 1), VersionShort.parse("10.1.1"));
		assertEquals(VersionShort.toInt(10, 1, 0), VersionShort.parse("10.1"));
		assertEquals(VersionShort.toInt(10, 1, 1), VersionShort.parse("xxx 10.1.1 xxx"));

		// test "raw" version str  and  generated ver str from integer
		assertEquals( "1.23.45", VersionShort.toStr(12345));
		assertEquals( "3.3.10",  VersionShort.toStr(30310));
	}
}
