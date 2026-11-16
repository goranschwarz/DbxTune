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
package com.dbxtune.cm.sqlserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Arrays;

import org.junit.Test;

import com.dbxtune.cm.sqlserver.CmDeadlocks.Deadlock;
import com.dbxtune.cm.sqlserver.CmDeadlocks.DlProcess;

/**
 * Parsing of real <code>xml_deadlock_report</code> events (captured from SQL Server 2022 and 2019 in Docker) and drawing them.
 */
public class CmDeadlocksTest
{
	private static String read(String name) throws Exception
	{
		try (InputStream in = CmDeadlocksTest.class.getResourceAsStream("deadlocks/" + name))
		{
			assertNotNull("test file not found: " + name, in);
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static Deadlock parse(String name) throws Exception
	{
		return Deadlock.parseEvent(read(name), 0);
	}

	@Test
	public void procKeylock2022() throws Exception
	{
		Deadlock d = parse("sql2022_proc_keylock.xml");

		assertEquals(Timestamp.valueOf("2026-11-15 20:51:23.0"), d.eventTime);
		assertEquals("dlk", d.getVictimDbName());
		assertEquals(Arrays.asList(79, 80), d.getSpids());
		assertEquals(79, d.getVictim().spid);
		assertEquals("dlk.dbo.p_customer_then_order (line 6)", d.getVictim().getProcNameAndLine());
		assertEquals("update dbo.orders    set amount  = amount-1  where id = @i", d.getVictim().getStatement());
		assertEquals("80", d.getOtherSpids());
		assertEquals("dlk.dbo.p_order_then_customer (line 6)", d.getOtherProcs());
		assertEquals("dlk.dbo.customers <-> dlk.dbo.orders", d.getObjects());
		assertEquals("keylock", d.getLockTypes());
		assertEquals("read committed (2)", d.getIsolationLevels());
		assertEquals(2425, d.getMaxWaitTimeMs());
		assertFalse(d.parallel);
		assertEquals(2, d.resources.size());
		assertEquals(1, d.resources.get(0).ownerIds.size());
		assertEquals(1, d.resources.get(0).waiterIds.size());

		// Native call stacks are removed, the rest is kept (so it can be opened in SSMS)
		assertFalse(d.xml.contains("<stackFrames>"));
		assertTrue(d.xml.startsWith("<deadlock>"));
		assertTrue(d.xml.endsWith("</deadlock>"));
		assertTrue(d.xml.contains("<inputbuf>"));
	}

	/** Same procedures on 2019 and 2022 = same shape key (other spids, other times, other index names) */
	@Test
	public void sameShapeAcrossVersions() throws Exception
	{
		Deadlock d22 = parse("sql2022_proc_keylock.xml");
		Deadlock d19 = parse("sql2019_proc_keylock.xml");

		assertEquals(d22.getShapeKey(), d19.getShapeKey());
		assertTrue( ! d22.getShapeKey().equals(parse("sql2022_3way_adhoc_keylock.xml").getShapeKey()) );
		assertEquals(16, d22.getShapeKey().length());
	}

	/** Ad hoc SQL: the frame text is "unknown", so the input buffer is used */
	@Test
	public void threeWayAdhoc() throws Exception
	{
		Deadlock d = parse("sql2022_3way_adhoc_keylock.xml");

		assertEquals(3, d.getSpids().size());
		assertEquals("dlk.dbo.ta <-> dlk.dbo.tb <-> dlk.dbo.tc", d.getObjects());
		assertNull(d.getVictim().procName);
		assertNull(d.getVictim().stmtText);
		assertTrue(d.getVictim().getStatement().contains("update dlk.dbo.tb set v=v+1 where id=1"));
		assertNull(d.getOtherProcs());
		// one statement per other spid
		assertEquals(2, d.getOtherStatements().split("-- spid ").length - 1);
	}

	@Test
	public void objectLockSerializable() throws Exception
	{
		Deadlock d = parse("sql2022_objectlock_serializable.xml");

		assertEquals("objectlock", d.getLockTypes());
		assertEquals("serializable (4)", d.getIsolationLevels());
		assertEquals("dlk.dbo.heap1 <-> dlk.dbo.heap2", d.getObjects());
	}

	/** SQL to look up a waitresource as it is now */
	@Test
	public void waitResourceSql() throws Exception
	{
		// KEY, object + index from the keylock: the row by %%lockres%%
		Deadlock d = parse("sql2022_proc_keylock.xml");
		DlProcess v = d.getVictim();
		String sql = d.getWaitResourceSql(v);
		assertTrue(sql, sql.contains("SELECT * FROM [dlk].[dbo].[orders] WITH (NOLOCK, INDEX([PK__orders__3213E83F117CD434])) WHERE %%lockres%% = '(40fd182c0dd9)'"));
		assertTrue(sql, sql.contains("-- NOTE: this is how it looks NOW"));

		// KEY without a known object: hobt_id -> table/index
		d.resources.forEach(r -> r.objectName = null);
		sql = d.getWaitResourceSql(v);
		assertTrue(sql, sql.contains("WHERE p.hobt_id = 72057594045923328"));

		// RID, PAGE
		d = parse("sql2022_proc_keylock.xml");
		v = d.getVictim();
		v.waitResource = "RID: 6:1:280:3";
		assertTrue(d.getWaitResourceSql(v).contains("SELECT * FROM [dlk].[dbo].[orders] WITH (NOLOCK) WHERE sys.fn_PhysLocFormatter(%%physloc%%) = '(1:280:3)'"));
		v.waitResource = "PAGE: 6:1:280 ";
		assertTrue(d.getWaitResourceSql(v).contains("FROM sys.dm_db_page_info(6, 1, 280, 'LIMITED')"));

		// OBJECT
		d = parse("sql2022_objectlock_serializable.xml");
		assertTrue(d.getWaitResourceSql(d.getVictim()).contains("SELECT DB_NAME(6) AS dbname, OBJECT_SCHEMA_NAME(1365579903, 6) AS schema_name, OBJECT_NAME(1365579903, 6) AS object_name"));

		// no wait / not data
		DlProcess noWait = new DlProcess();
		assertEquals(null, d.getWaitResourceSql(noWait));
		noWait.waitResource = "METADATA: database_id = 6 SECURITY_CACHE($hash = 0x1:0x0)";
		assertEquals(null, d.getWaitResourceSql(noWait));
	}

	/** Parallel query deadlock: one spid, several ecids, exchange events (no objects) */
	@Test
	public void parallel() throws Exception
	{
		Deadlock d = parse("parallel_exchange_synthetic.xml");

		assertTrue(d.parallel);
		assertEquals(Arrays.asList(60), d.getSpids());
		assertEquals("(parallel query)", d.getObjects());
		assertEquals("exchangeEvent", d.getLockTypes());
		assertEquals("", d.getOtherSpids());
		DlProcess v = d.getVictim();
		assertEquals(0, v.ecid);
		assertEquals("ReportApp", v.app);
	}

	/** The event time is UTC, the times inside the report are server local time */
	@Test
	public void utcOffset() throws Exception
	{
		Deadlock d = Deadlock.parseEvent(read("sql2022_proc_keylock.xml"), 120);
		assertEquals(Timestamp.valueOf("2026-11-15 22:51:23.0"), d.eventTime);
		assertTrue(d.getDeadlockId().startsWith("2026-11-15T20:51:23Z/"));
	}

	/** The saved XML (column DeadlockXml) can be parsed again, as the DSR does */
	@Test
	public void reparseSavedXml() throws Exception
	{
		Deadlock d  = parse("sql2022_3way_adhoc_keylock.xml");
		Deadlock d2 = Deadlock.parseDeadlock(d.xml, d.eventTime);
		assertEquals(d.getShapeKey(), d2.getShapeKey());
		assertEquals(d.getObjects(),  d2.getObjects());
	}

	/** No DOCTYPE / external entities */
	@Test
	public void rejectsDoctype() throws Exception
	{
		String evil = "<event timestamp=\"2026-11-15T10:00:00Z\"><!DOCTYPE x [<!ENTITY e SYSTEM \"file:///etc/passwd\">]><deadlock>&e;</deadlock></event>";
		try
		{
			Deadlock.parseEvent(evil, 0);
			fail("A DOCTYPE should be rejected");
		}
		catch (Exception expected)
		{
		}
	}

	@Test
	public void paintAll() throws Exception
	{
		for (String name : new String[] {"sql2022_proc_keylock.xml", "sql2019_proc_keylock.xml", "sql2022_3way_adhoc_keylock.xml", "sql2022_objectlock_serializable.xml", "parallel_exchange_synthetic.xml"})
		{
			BufferedImage img = SqlServerDeadlockTimelinePainter.paint(parse(name), 3);
			assertEquals(960, img.getWidth());
			assertTrue(name, img.getHeight() > 100);

			String html = SqlServerDeadlockTimelinePainter.toHtmlImg(parse(name), 1);
			assertTrue(name, html.startsWith("<img width='960'"));
		}
	}

	@Test
	public void formatMs()
	{
		assertEquals("48 ms",        SqlServerDeadlockTimelinePainter.formatMs(48));
		assertEquals("2.43 s",       SqlServerDeadlockTimelinePainter.formatMs(2425));
		assertEquals("12.4 s",       SqlServerDeadlockTimelinePainter.formatMs(12_425));
		assertEquals("3 min 12 s",   SqlServerDeadlockTimelinePainter.formatMs(192_000));
		assertEquals("2 h 13 min",   SqlServerDeadlockTimelinePainter.formatMs(7_980_000));
	}
}
