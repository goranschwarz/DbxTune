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
package com.dbxtune.pcs.report.content.postgres;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

/** The parser for the text of pg_get_indexdef(), used by Index Analysis */
public class PostgresAbstractIndexDdlTest
{
	private static PostgresAbstract.PgIndexDdl p(String ddl) { return PostgresAbstract.parsePgIndexDdl(ddl); }

	@Test public void plain()
	{
		PostgresAbstract.PgIndexDdl d = p("CREATE INDEX ix_a ON public.t USING btree (a)");
		assertFalse(d.unique);
		assertEquals("ix_a", d.name);
		assertEquals("public.t", d.table);
		assertEquals("btree", d.method);
		assertEquals(Arrays.asList("a"), d.keys);
		assertTrue(d.includes.isEmpty());
		assertNull(d.where);
	}

	@Test public void uniqueDescNullsIncludeWithTablespace()
	{
		PostgresAbstract.PgIndexDdl d = p("CREATE UNIQUE INDEX ix_b ON s1.t USING btree (a, b DESC, c NULLS FIRST) INCLUDE (x, y) WITH (fillfactor='80') TABLESPACE ts1");
		assertTrue(d.unique);
		assertEquals(Arrays.asList("a", "b DESC", "c NULLS FIRST"), d.keys);
		assertEquals(Arrays.asList("x", "y"), d.includes);
		assertEquals("fillfactor='80'", d.with);
		assertEquals("ts1", d.tablespace);
	}

	@Test public void partial()
	{
		PostgresAbstract.PgIndexDdl d = p("CREATE INDEX ix_p ON public.t USING btree (a) WHERE (b > 0)");
		assertEquals("(b > 0)", d.where);
	}

	@Test public void expressionKeepsParentheses()
	{
		PostgresAbstract.PgIndexDdl d = p("CREATE INDEX ix_e ON public.t USING btree (lower((name)::text), a)");
		assertEquals(Arrays.asList("lower((name)::text)", "a"), d.keys);
	}

	@Test public void quotedNamesAndOtherMethod()
	{
		PostgresAbstract.PgIndexDdl d = p("CREATE INDEX \"Ix Mixed\" ON \"My Schema\".\"T\" USING gin (\"Col, 1\")");
		assertEquals("\"Ix Mixed\"", d.name);
		assertEquals("gin", d.method);
		assertEquals(Arrays.asList("\"Col, 1\""), d.keys);
	}

	@Test public void onlyIsPartitioned()
	{
		assertTrue(p("CREATE INDEX ix_pt ON ONLY public.pt USING btree (a)").only);
	}

	@Test public void notAnIndexDefinition()
	{
		assertNull(p(null));
		assertNull(p("CREATE TABLE t (a int)"));
		assertNull(p("CREATE INDEX ix ON public.t USING btree (a"));         // no closing parenthesis
		assertNull(p("CREATE INDEX ix ON public.t USING btree (a) SOMETHING")); // unknown tail
	}
}
