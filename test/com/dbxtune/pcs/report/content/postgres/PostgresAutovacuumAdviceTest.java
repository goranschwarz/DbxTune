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
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;

public class PostgresAutovacuumAdviceTest
{
	/** The Postgres 17 defaults, as MonSessionDbmsConfig has them */
	private static Map<String, String> server17()
	{
		Map<String, String> m = new LinkedHashMap<>();
		m.put("autovacuum", "on");
		m.put("autovacuum_vacuum_threshold", "50");
		m.put("autovacuum_vacuum_scale_factor", "0.2");
		m.put("autovacuum_vacuum_insert_threshold", "1000");
		m.put("autovacuum_vacuum_insert_scale_factor", "0.2");
		m.put("autovacuum_analyze_threshold", "50");
		m.put("autovacuum_analyze_scale_factor", "0.1");
		m.put("autovacuum_vacuum_cost_limit", "-1");
		m.put("autovacuum_vacuum_cost_delay", "2");
		m.put("autovacuum_freeze_max_age", "200000000");
		m.put("autovacuum_multixact_freeze_max_age", "400000000");
		m.put("vacuum_cost_limit", "200");
		return m;
	}

	/** An advice with settings, and a "quiet" day: 100 samples, nothing above anything */
	private static PostgresAutovacuumAdvice advice(Map<String, String> server, String reloptions, long rows)
	{
		PostgresAutovacuumAdvice a = new PostgresAutovacuumAdvice();
		a.schemaName = "public";
		a.tableName  = "t1";
		a.setSettings(server, PostgresAutovacuumAdvice.parseReloptions(reloptions));
		a.liveRows   = rows;
		a.samples    = 100;
		a.firstSample = Timestamp.valueOf("2026-11-14 00:00:00");
		a.lastSample  = Timestamp.valueOf("2026-11-14 23:59:50");
		a.samplesAboveInsert = 0;
		a.maxInsSinceVacuum  = 0;
		a.frozenxidAge = 1000;
		a.minmxidAge   = 0;
		return a;
	}

	private static void analyze(PostgresAutovacuumAdvice a)
	{
		a.analyze(25, 1_000_000, 50);
	}

	private static List<String> rules(PostgresAutovacuumAdvice a)
	{
		return a.findings.stream().map(f -> f.rule).collect(Collectors.toList());
	}

	@Test public void triggerPoints()
	{
		PostgresAutovacuumAdvice a = advice(server17(), null, 1_000_000);
		assertEquals(50 + 0.2 * 1_000_000,   a.vacuumTrigger(1_000_000),  0.001);
		assertEquals(1000 + 0.2 * 1_000_000, a.insertTrigger(1_000_000),  0.001);
		assertEquals(50 + 0.1 * 1_000_000,   a.analyzeTrigger(1_000_000), 0.001);
	}

	@Test public void maxThreshold18()
	{
		Map<String, String> s = server17();
		s.put("autovacuum_vacuum_max_threshold", "100000000");
		PostgresAutovacuumAdvice a = advice(s, null, 1_000_000_000L);
		assertEquals(100_000_000, a.vacuumTrigger(1_000_000_000L), 0.001); // 0.2 * 1 billion is capped
	}

	@Test public void reloptionsOverrideServer()
	{
		PostgresAutovacuumAdvice a = advice(server17(), "autovacuum_vacuum_scale_factor=0.01, fillfactor=90", 1_000_000);
		assertEquals("0.01", a.settings.get("autovacuum_vacuum_scale_factor").effective());
		assertEquals("0.2",  a.settings.get("autovacuum_vacuum_scale_factor").serverValue);
		assertEquals(50 + 0.01 * 1_000_000, a.vacuumTrigger(1_000_000), 0.001);
		// cost limit -1 = vacuum_cost_limit
		assertEquals("200", a.settings.get("autovacuum_vacuum_cost_limit").effective());
	}

	@Test public void noInsertSettingBefore13()
	{
		Map<String, String> s = server17();
		s.remove("autovacuum_vacuum_insert_threshold");
		s.remove("autovacuum_vacuum_insert_scale_factor");
		PostgresAutovacuumAdvice a = advice(s, null, 10);
		assertFalse(a.has("autovacuum_vacuum_insert_threshold"));
	}

	@Test public void ok()
	{
		PostgresAutovacuumAdvice a = advice(server17(), null, 5_000_000);
		a.maxDeadPct = 3;
		analyze(a);
		assertTrue(a.findings.isEmpty());
		assertEquals("OK", a.getShortVerdict());
		assertEquals("", a.getScript());
	}

	@Test public void r0_disabled()
	{
		PostgresAutovacuumAdvice a = advice(server17(), "autovacuum_enabled=false", 1000);
		analyze(a);
		assertEquals(Arrays.asList("R0"), rules(a));
		// the table had the option: the rollback SETs it back
		String script = a.getScript();
		assertTrue(script, script.contains("ALTER TABLE public.t1 SET (autovacuum_enabled = on);"));
		assertTrue(script, script.contains("ALTER TABLE public.t1 SET (autovacuum_enabled = false);"));
	}

	@Test public void r1_notKeepingUp()
	{
		PostgresAutovacuumAdvice a = advice(server17(), null, 10_000);
		a.samplesAboveVacuum = 40;  // 40% of the samples
		analyze(a);
		assertEquals(Arrays.asList("R1"), rules(a));
		assertEquals("1000", a.suggested.get("autovacuum_vacuum_cost_limit"));
		assertTrue(a.getScript().contains("RESET (autovacuum_vacuum_cost_limit)"));
	}

	@Test public void r1_notWhenAutovacuumRan()
	{
		PostgresAutovacuumAdvice a = advice(server17(), null, 10_000);
		a.samplesAboveVacuum = 40;
		a.autovacuums.add(Timestamp.valueOf("2026-11-14 12:00:00"));
		analyze(a);
		assertFalse(rules(a).contains("R1"));
	}

	@Test public void r2_bigTableTriggerTooHigh()
	{
		PostgresAutovacuumAdvice a = advice(server17(), null, 12_000_000);
		a.maxDeadPct = 15;
		analyze(a);
		assertEquals(Arrays.asList("R2"), rules(a));
		assertEquals("0.01", a.suggested.get("autovacuum_vacuum_scale_factor")); // > 10M rows
		assertEquals("1000", a.suggested.get("autovacuum_vacuum_threshold"));
		String script = a.getScript();
		assertTrue(script, script.contains("ALTER TABLE public.t1 SET (autovacuum_vacuum_scale_factor = 0.01, autovacuum_vacuum_threshold = 1000);"));
		assertTrue(script, script.contains("ALTER TABLE public.t1 RESET (autovacuum_vacuum_scale_factor, autovacuum_vacuum_threshold);"));
	}

	@Test public void r2_notWhenScaleFactorAlreadyLow()
	{
		PostgresAutovacuumAdvice a = advice(server17(), "autovacuum_vacuum_scale_factor=0.02", 12_000_000);
		a.maxDeadPct = 15;
		analyze(a);
		assertFalse(rules(a).contains("R2"));
	}

	@Test public void r3_appendOnly()
	{
		PostgresAutovacuumAdvice a = advice(server17(), null, 2_000_000);
		a.maxInsSinceVacuum = 300_000; // 15% of the rows
		a.maxDeadPct = 0;
		analyze(a);
		assertEquals(Arrays.asList("R3"), rules(a));
		assertEquals("0.02", a.suggested.get("autovacuum_vacuum_insert_scale_factor"));
	}

	@Test public void r4_cleanupBlocked()
	{
		PostgresAutovacuumAdvice a = advice(server17(), null, 10_000);
		a.autovacuums.add(Timestamp.valueOf("2026-11-14 10:00:00"));
		a.autovacuums.add(Timestamp.valueOf("2026-11-14 11:00:00"));
		a.minDeadRows = 5000; // trigger = 50 + 0.2 * 10000 = 2050
		analyze(a);
		assertEquals(Arrays.asList("R4"), rules(a));
		assertTrue(a.suggested.isEmpty()); // settings do not help
	}

	@Test public void r5_staleStatistics()
	{
		PostgresAutovacuumAdvice a = advice(server17(), null, 3_000_000);
		a.samplesAboveAnalyze = 30;
		analyze(a);
		assertEquals(Arrays.asList("R5"), rules(a));
		assertEquals("0.02", a.suggested.get("autovacuum_analyze_scale_factor"));
	}

	@Test public void r6_freeze()
	{
		PostgresAutovacuumAdvice a = advice(server17(), "autovacuum_freeze_max_age=100000", 2_000_000);
		a.frozenxidAge = 85_000; // 85% of the table's own freeze_max_age
		analyze(a);
		assertEquals(Arrays.asList("R6"), rules(a));
		assertTrue(a.findings.get(0).verdict.contains("near"));
		assertTrue(a.getScript().contains("VACUUM (FREEZE, VERBOSE) public.t1;"));
	}

	@Test public void r6_notForSmallTable()
	{
		// the forced vacuum of a small table takes a moment: nothing to act on
		PostgresAutovacuumAdvice a = advice(server17(), "autovacuum_freeze_max_age=100000", 1000);
		a.frozenxidAge = 97_000;
		analyze(a);
		assertTrue(rules(a).isEmpty());
	}

	@Test public void triggerFromRelTuples()
	{
		// Postgres computes the start points from pg_class.reltuples, -1 (never vacuumed/analyzed) = 0
		PostgresAutovacuumAdvice a = advice(server17(), null, 50_000);
		assertEquals(50_000, a.baseRows()); // an old recording without row_estimate: n_live_tup
		a.hasRelTuples = true;
		a.relTuples    = 200_000;
		assertEquals(200_000, a.baseRows());
		a.relTuples    = -1;
		assertEquals(0, a.baseRows());
	}

	@Test public void r1_oldDeadRows()
	{
		// nothing changed during the recording: the dead rows are from before, no cost limit suggestion, a VACUUM by hand
		PostgresAutovacuumAdvice a = advice(server17(), null, 10_000);
		a.samplesAboveVacuum = 100;
		a.noChanges = true;
		analyze(a);
		assertEquals(Arrays.asList("R1"), rules(a));
		assertTrue(a.findings.get(0).verdict.contains("old dead rows"));
		assertTrue(a.suggested.isEmpty());
		assertTrue(a.getScript().contains("VACUUM (ANALYZE, VERBOSE) public.t1;"));
	}

	@Test public void r7_oldRowEstimate()
	{
		PostgresAutovacuumAdvice a = advice(server17(), null, 53_000);
		a.hasRelTuples = true;
		a.relTuples    = 800_000;
		analyze(a);
		assertEquals(Arrays.asList("R7"), rules(a));
		assertTrue(a.getScript().contains("ANALYZE (VERBOSE) public.t1;"));

		// close enough: nothing
		PostgresAutovacuumAdvice b = advice(server17(), null, 53_000);
		b.hasRelTuples = true;
		b.relTuples    = 60_000;
		analyze(b);
		assertTrue(rules(b).isEmpty());
	}

	@Test public void quotedNames()
	{
		PostgresAutovacuumAdvice a = advice(server17(), "autovacuum_enabled=false", 1000);
		a.schemaName = "Sales";
		a.tableName  = "order \"lines\"";
		analyze(a);
		assertTrue(a.getScript(), a.getScript().contains("ALTER TABLE \"Sales\".\"order \"\"lines\"\"\" SET"));
	}

	@Test public void longestGap()
	{
		Timestamp first = Timestamp.valueOf("2026-11-14 00:00:00");
		Timestamp last  = Timestamp.valueOf("2026-11-14 10:00:00");
		assertEquals(36000, PostgresAutovacuumAdvice.longestGapSec(first, last, Arrays.asList()));
		assertEquals(6 * 3600, PostgresAutovacuumAdvice.longestGapSec(first, last, Arrays.asList(Timestamp.valueOf("2026-11-14 02:00:00"), Timestamp.valueOf("2026-11-14 08:00:00"))));
	}

	@Test public void htmlIsEscaped()
	{
		PostgresAutovacuumAdvice a = advice(server17(), "autovacuum_enabled=false", 1000);
		a.tableName = "<script>";
		analyze(a);
		String html = a.toHtml();
		assertFalse(html.contains("<script>"));
		assertTrue(html.contains("autovacuum is OFF for this table"));
	}
}
