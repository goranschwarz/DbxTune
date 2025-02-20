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

public class JsonUtilsTest
{
	@Test
	public void isPossibleJson()
	{
		assertEquals("Blank or null should return false", false, JsonUtils.isPossibleJson(null));
		assertEquals("Blank or null should return false", false, JsonUtils.isPossibleJson(""));

		assertEquals("Not A JSON String",                 false, JsonUtils.isPossibleJson("   not a json"));

		assertEquals("Not A JSON String",            false, JsonUtils.isPossibleJson("{}"));
		assertEquals("Not A JSON String",            false, JsonUtils.isPossibleJson("[]"));
		assertEquals("Not A JSON String",            false, JsonUtils.isPossibleJson("#key#:#value#"               .replace('#', '"')));
		assertEquals("Not A JSON String",            false, JsonUtils.isPossibleJson("  xx    #{key#:#value#}"     .replace('#', '"')));
		assertEquals("Not A JSON String",            false, JsonUtils.isPossibleJson("        #{key#=#value#}"     .replace('#', '"')));

		assertEquals("Not A JSON String, but close", false, JsonUtils.isPossibleJson("#{key#:#value#}"             .replace('#', '"')));
		assertEquals("Not A JSON String, but close", false, JsonUtils.isPossibleJson("      #{key#:#value#}"       .replace('#', '"')));

		assertEquals("A JSON String",                true, JsonUtils.isPossibleJson("{#key#:#value#}"              .replace('#', '"')));
		assertEquals("A JSON String",                true, JsonUtils.isPossibleJson("      {#key#:#value#}"        .replace('#', '"')));
		assertEquals("A JSON String",                true, JsonUtils.isPossibleJson("  [    {#key#:#value#}    ]"  .replace('#', '"')));

		assertEquals("A JSON String",                true, JsonUtils.isPossibleJson("[ {#serverName#:#SYB_MXG_P01#, #to#:#sybase-dba-alarms@b3.se, lafo@sek.se#}, {#serverName#:#SYB_SEK_MXGT0#, #to#:#lafo@sek.se#} ]".replace('#', '"')));

		assertEquals("Not A JSON String",            false, JsonUtils.isPossibleJson("xxx"));
		assertEquals("Not A JSON String",            false, JsonUtils.isPossibleJson("name@acme.com"));
	}


	@Test
	public void isJsonValid()
	{
		assertEquals("Blank or null should return false", false, JsonUtils.isJsonValid(null));
		assertEquals("Blank or null should return false", false, JsonUtils.isJsonValid(""));

		assertEquals("Not A JSON String",                 false, JsonUtils.isJsonValid("   not a json"));

//		assertEquals("Not A JSON String",            false, JsonUtils.isJsonValid("{}"));
//		assertEquals("Not A JSON String",            false, JsonUtils.isJsonValid("[]"));
		assertEquals("Not A JSON String",            false, JsonUtils.isJsonValid("#key#:#value#"               .replace('#', '"')));
		assertEquals("Not A JSON String",            false, JsonUtils.isJsonValid("  xx    #{key#:#value#}"     .replace('#', '"')));
		assertEquals("Not A JSON String",            false, JsonUtils.isJsonValid("        #{key#=#value#}"     .replace('#', '"')));

		assertEquals("Not A JSON String, but close", false, JsonUtils.isJsonValid("#{key#:#value#}"             .replace('#', '"')));
		assertEquals("Not A JSON String, but close", false, JsonUtils.isJsonValid("      #{key#:#value#}"       .replace('#', '"')));

		assertEquals("A JSON String",                true,  JsonUtils.isJsonValid("{#key#:#value#}"              .replace('#', '"')));
		assertEquals("A JSON String",                true,  JsonUtils.isJsonValid("      {#key#:#value#}"        .replace('#', '"')));
		assertEquals("A JSON String",                true,  JsonUtils.isJsonValid("  [    {#key#:#value#}    ]"  .replace('#', '"')));

		assertEquals("A JSON String",                true,  JsonUtils.isJsonValid("[ {#serverName#:#SYB_MXG_P01#, #to#:#sybase-dba-alarms@b3.se, lafo@sek.se#}, {#serverName#:#SYB_SEK_MXGT0#, #to#:#lafo@sek.se#} ]".replace('#', '"')));

		assertEquals("Not A JSON String",            false, JsonUtils.isJsonValid("xxx"));
		assertEquals("Not A JSON String",            false, JsonUtils.isJsonValid("name@acme.com"));
	}


}
