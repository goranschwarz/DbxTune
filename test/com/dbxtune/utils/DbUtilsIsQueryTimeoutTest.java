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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;

import org.junit.Test;

public class DbUtilsIsQueryTimeoutTest
{
	private static SQLException ex(String text, String state, int code)
	{
		return new SQLException(text, state, code);
	}

	@Test
	public void testTheJdbcStandard()
	{
		assertTrue(DbUtils.isQueryTimeout(new SQLTimeoutException("any text")));
	}

	@Test
	public void testVendors()
	{
		assertTrue(DbUtils.isQueryTimeout(ex("ERROR: canceling statement due to user request", "57014", 0))); // Postgres
		assertTrue(DbUtils.isQueryTimeout(ex("[jcc][t4][20144][14162][4.38.7] Command timed out. ERRORCODE=-952, SQLSTATE=57014", "57014", -952))); // Db2: CommandTimeoutSQLException, which isn't a SQLTimeoutException
		assertTrue(DbUtils.isQueryTimeout(ex("The query has timed out.", "HY008", 0)));                        // SQL Server
		assertTrue(DbUtils.isQueryTimeout(ex("Query execution was interrupted", "70100", 1317)));              // MySQL
		assertTrue(DbUtils.isQueryTimeout(ex("Query execution was interrupted, maximum statement execution time exceeded", "HY000", 3024))); // MySQL
		assertTrue(DbUtils.isQueryTimeout(ex("ORA-01013: user requested cancel of current operation", "72000", 1013))); // Oracle
	}

	@Test
	public void testJConnect()
	{
		// What jConnect 16.0 SP04 really throws
		SQLException jz0to = ex("JZ0TO: Read operation timed out.", "JZ0TO", 0);
		jz0to.initCause(new SocketTimeoutException("Read timed out"));
		SQLException real = ex("JZ006: Caught IOException: java.net.SocketTimeoutException: Read timed out use getCause() to see the error chain", "JZ006", 0);
		real.initCause(jz0to);
		real.setNextException(ex("JZ0TO: Read operation timed out.", "JZ0TO", 0));
		assertTrue(DbUtils.isQueryTimeout(real));

		assertTrue(DbUtils.isQueryTimeout(ex("JZ0T3: Read operation timed out.", "JZ0T3", 0)));
		assertTrue(DbUtils.isQueryTimeout(ex("JZ0TO: Read operation timed out.", "JZ0TO", 0)));
		assertTrue(DbUtils.isQueryTimeout(ex("JZ0T4: Write operation timed out.  Timeout in milliseconds: 1000.", "JZ0T4", 0)));
		assertTrue(DbUtils.isQueryTimeout(ex("JZ006: Caught IOException: java.io.IOException: JZ0T3: Read operation timed out.", "JZ006", 0)));
		assertTrue(DbUtils.isQueryTimeout(ex("JZ006: Se ha detectado IOException: java.net.SocketTimeoutException: Read timed out.", "JZ006", 0)));

		assertFalse(DbUtils.isQueryTimeout(ex("JZ006: Caught IOException: com.sybase.jdbc4.jdbc.SybConnectionDeadException: JZ0C0: Connection is already closed.", "JZ006", 0)));
	}

	@Test
	public void testCauseAndNextException()
	{
		SQLException cause = new SQLException("Wrapper");
		cause.initCause(new IOException("I/O", new SocketTimeoutException("Read timed out")));
		assertTrue(DbUtils.isQueryTimeout(cause));

		SQLException next = new SQLException("First");
		next.setNextException(ex("The query has timed out.", "HY008", 0));
		assertTrue(DbUtils.isQueryTimeout(next));
	}

	@Test
	public void testOtherErrors()
	{
		assertFalse(DbUtils.isQueryTimeout(null));
		assertFalse(DbUtils.isQueryTimeout(new SQLException("no state or code")));
		assertFalse(DbUtils.isQueryTimeout(ex("Incorrect syntax near 'master'.", "ZZZZZ", 102)));
		assertFalse(DbUtils.isQueryTimeout(ex("Lock request time out period exceeded.", "S0001", 1222))); // SQL Server: a lock, not our timeout

		// The same numbers on other vendors
		assertFalse(DbUtils.isQueryTimeout(ex("Can't get status of 't1'", "HY000", 1013)));
		assertFalse(DbUtils.isQueryTimeout(ex("Write on some file failed", "S0001", 3024)));

		// The text isn't used
		assertFalse(DbUtils.isQueryTimeout(ex("Someone wrote: timed out", "42000", 1)));
	}
}
