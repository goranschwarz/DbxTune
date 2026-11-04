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
package com.dbxtune.pcs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.dbxtune.sql.conn.DbxConnection;

/**
 * An object that is already stored, and then is looked up under another name, must be marked as stored under that name as well.
 * Otherwise: PersistentCounterHandler does the whole lookup (sp_help, sp_spaceused) every time that name is requested.
 */
public class PersistWriterJdbcDdlCacheTest
{
	private Connection        _conn;
	private PersistWriterJdbc _pw;

	@Before
	public void init() throws Exception
	{
		_conn = DriverManager.getConnection("jdbc:h2:mem:ddlCacheTest");
		_pw = new PersistWriterJdbc();
		_pw._mainConn       = DbxConnection.createDbxConnection(_conn);
		_pw._ddlStorageConn = _pw._mainConn; // By default the DDL Storage has its own connection
		_pw.setSessionStarted(true);

		// The object is already stored
		_pw.markDdlDetailsAsStored("shop", "orders");
		_pw.markDdlDetailsAsStored("shop", "dbo.orders");
	}

	@After
	public void close() throws Exception
	{
		_conn.close();
	}

	private static DdlDetails lookup(String searchDbname, String searchObjectName)
	{
		DdlDetails entry = new DdlDetails();
		entry.setDbname          ("shop");
		entry.setOwner           ("dbo");
		entry.setObjectName      ("orders");
		entry.setSearchDbname    (searchDbname);
		entry.setSearchObjectName(searchObjectName);
		return entry;
	}

	@Test
	public void testSearchedInOtherDatabase()
	{
		assertFalse(_pw.isDdlDetailsStored("tempdb", "orders"));

		_pw.saveDdlDetails(lookup("tempdb", "orders"));

		assertTrue(_pw.isDdlDetailsStored("tempdb", "orders"));
	}

	@Test
	public void testSearchedWithOtherName()
	{
		assertFalse(_pw.isDdlDetailsStored("shop", "shop..orders"));

		_pw.saveDdlDetails(lookup("shop", "shop..orders"));

		assertTrue(_pw.isDdlDetailsStored("shop", "shop..orders"));
	}

	@Test
	public void testSaveReturnsFalseWhenAlreadyStored()
	{
		assertFalse(_pw.saveDdlDetails(lookup("tempdb", "orders")));
	}

	@Test
	public void testSaveReturnsTrueOnlyWhenWritten() throws Exception
	{
		try (Statement stmt = _conn.createStatement())
		{
			for (String ddl : PersistWriterBase.getTableDdlString(_pw._mainConn, null, PersistWriterBase.DDL_STORAGE, null))
				stmt.executeUpdate(ddl);
		}

		DdlDetails entry = new DdlDetails();
		entry.setDbname          ("shop");
		entry.setOwner           ("dbo");
		entry.setObjectName      ("customers");
		entry.setSearchDbname    ("shop");
		entry.setSearchObjectName("customers");
		entry.setType            ("U");
		entry.setObjectText      ("some text");
		entry.setSampleTime      (new Timestamp(System.currentTimeMillis()));

		assertTrue (_pw.saveDdlDetails(entry));
		assertFalse(_pw.saveDdlDetails(entry));

		try (Statement stmt = _conn.createStatement(); ResultSet rs = stmt.executeQuery("select count(*) from \"MonDdlStorage\""))
		{
			rs.next();
			assertEquals(1, rs.getInt(1));
		}
	}

	@Test
	public void testSaveReturnsFalseOnWriteError()
	{
		// The table does not exist in this test
		DdlDetails entry = lookup("shop", "customers");
		entry.setObjectName("customers");
		assertFalse(_pw.saveDdlDetails(entry));
	}
}
