/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
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
package com.dbxtune.utils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.sql.Connection;
import java.sql.DriverManager;

import org.junit.Test;

public class AseSqlScriptLastSqlExceptionTest
{
	@Test
	public void testErrorWrittenToTheTextIsRemembered() throws Exception
	{
		try (Connection h2 = DriverManager.getConnection("jdbc:h2:mem:aseSqlScriptTest");
		     AseSqlScript ss = new AseSqlScript(h2, 10, false))
		{
			ss.setUseGlobalMsgHandler(false);

			ss.executeSqlStr("select * from table_that_does_not_exist", true);
			assertNotNull(ss.getLastSqlException());

			// A new execution starts over
			ss.executeSqlStr("select 1", true);
			assertNull(ss.getLastSqlException());
		}
	}
}
