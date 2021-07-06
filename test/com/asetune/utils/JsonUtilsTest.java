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
package com.asetune.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class JsonUtilsTest
{
	@Test
	public void simpleTest()
	{
		assertEquals("Blank or null should return false", false, JsonUtils.isPossibleJson(null));
		assertEquals("Blank or null should return false", false, JsonUtils.isPossibleJson(""));

		assertEquals("Not A JSON String",                 false, JsonUtils.isPossibleJson("   not a json"));

		assertEquals("Not A JSON String",            false, JsonUtils.isPossibleJson("{}"));
		assertEquals("Not A JSON String",            false, JsonUtils.isPossibleJson("[]"));
		assertEquals("Not A JSON String",            false, JsonUtils.isPossibleJson("\"key\":\"value\""));
		assertEquals("Not A JSON String",            false, JsonUtils.isPossibleJson("  xx    \"{key\":\"value\"}"));
		assertEquals("Not A JSON String",            false, JsonUtils.isPossibleJson("        \"{key\"=\"value\"}"));

		assertEquals("Not A JSON String, but close", false, JsonUtils.isPossibleJson("\"{key\":\"value\"}"));
		assertEquals("Not A JSON String, but close", false, JsonUtils.isPossibleJson("      \"{key\":\"value\"}"));

		assertEquals("A JSON String",                true, JsonUtils.isPossibleJson("{\"key\":\"value\"}"));
		assertEquals("A JSON String",                true, JsonUtils.isPossibleJson("      {\"key\":\"value\"}"));
		assertEquals("A JSON String",                true, JsonUtils.isPossibleJson("  [    {\"key\":\"value\"}    ]"));

		assertEquals("A JSON String",                true, JsonUtils.isPossibleJson("[ {\"serverName\":\"SYB_MXG_P01\", \"to\":\"sybase-dba-alarms@b3.se, lafo@sek.se\"}, {\"serverName\":\"SYB_SEK_MXGT0\", \"to\":\"lafo@sek.se\"} ]"));
		assertEquals("A JSON String",                true, JsonUtils.isJsonValid   ("[ {\"serverName\":\"SYB_MXG_P01\", \"to\":\"sybase-dba-alarms@b3.se, lafo@sek.se\"}, {\"serverName\":\"SYB_SEK_MXGT0\", \"to\":\"lafo@sek.se\"} ]"));
	}


}
