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
package com.dbxtune.loadgen;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Counters for a run.
 * <ul>
 *   <li>Per scenario execution ("unit"): count, errors, a millisecond histogram (for p95) - used for the summary</li>
 *   <li>Per minute and scenario+statement: count, errors, total/max ms, rows, session ids - written to the journal every minute</li>
 * </ul>
 */
public class Stats
{
	private static final Pattern QUOTED_STRING = Pattern.compile("'(?:[^']|'')*'");
	private static final Pattern NUMBER        = Pattern.compile("(?<![\\w.])-?\\d+(\\.\\d+)?(?![\\w.])");

	private final Journal _journal;
	private final Map<String, UnitTotals> _units = new ConcurrentHashMap<>();
	private final Set<String> _sqlSeen = ConcurrentHashMap.newKeySet();
	private Map<String, MinuteAgg> _minute = new ConcurrentHashMap<>();
	private OffsetDateTime _minuteStart = OffsetDateTime.now().truncatedTo(ChronoUnit.MINUTES);

	public Stats(Journal journal)
	{
		_journal = journal;
	}

	//------------------------------------------------------------------
	// Scenario units
	//------------------------------------------------------------------
	private static class UnitTotals
	{
		long count, errors, sumMs, maxMs;
		final int[] histo = new int[10_001]; // ms 0..9999, last slot = 10s and above
		String lastError;

		synchronized void add(long ms, String error)
		{
			count++;
			sumMs += ms;
			maxMs  = Math.max(maxMs, ms);
			histo[(int) Math.min(ms, 10_000)]++;
			if (error != null)
			{
				errors++;
				lastError = error;
			}
		}

		synchronized long p95()
		{
			long target = (long) Math.ceil(count * 0.95), sum = 0;
			for (int i=0; i<histo.length; i++)
			{
				sum += histo[i];
				if (sum >= target)
					return i == histo.length - 1 ? maxMs : i; // the last slot holds everything >= 10 s
			}
			return maxMs;
		}
	}

	/** One execution of a scenario (OLTP/OLAP/BATCH unit, or a problem event) */
	public void recordUnit(String scenario, long ms, String error)
	{
		_units.computeIfAbsent(scenario, k -> new UnitTotals()).add(ms, error);
	}

	public long getTotalCount()  { return _units.values().stream().mapToLong(u -> u.count ).sum(); }
	public long getTotalErrors() { return _units.values().stream().mapToLong(u -> u.errors).sum(); }

	/** The summary rows, one per scenario, sorted by name */
	public List<Map<String, Object>> getSummary()
	{
		List<Map<String, Object>> list = new ArrayList<>();
		for (Map.Entry<String, UnitTotals> e : new TreeMap<>(_units).entrySet())
		{
			UnitTotals u = e.getValue();
			synchronized (u)
			{
				Map<String, Object> m = new LinkedHashMap<>();
				m.put("scenario",  e.getKey());
				m.put("count",     u.count);
				m.put("errors",    u.errors);
				m.put("avgMs",     u.count == 0 ? 0 : u.sumMs / u.count);
				m.put("p95Ms",     u.p95());
				m.put("maxMs",     u.maxMs);
				m.put("lastError", u.lastError);
				list.add(m);
			}
		}
		return list;
	}

	//------------------------------------------------------------------
	// Statements, per minute
	//------------------------------------------------------------------
	private static class MinuteAgg
	{
		String scenario, sqlHash;
		long count, errors, sumMs, maxMs, rows;
		final Set<Integer> spids = new TreeSet<>();
		final Map<String, Integer> errorCodes = new TreeMap<>();

		synchronized void add(long ms, int rowCnt, SQLException ex, int spid)
		{
			count++;
			sumMs += ms;
			maxMs  = Math.max(maxMs, ms);
			rows  += rowCnt;
			if (spids.size() < 10)
				spids.add(spid);
			if (ex != null)
			{
				errors++;
				errorCodes.merge(ex.getErrorCode() + "/" + ex.getSQLState(), 1, Integer::sum);
			}
		}
	}

	/** Literals replaced with '?', so statements that only differ in literals are counted together */
	public static String normalize(String sql)
	{
		String s = QUOTED_STRING.matcher(sql).replaceAll("?");
		s = NUMBER.matcher(s).replaceAll("?");
		return s.replaceAll("\\s+", " ").trim();
	}

	public static String hash(String normalizedSql)
	{
		return String.format("%08x", normalizedSql.hashCode());
	}

	public void recordStatement(String scenario, String sql, long ms, int rows, SQLException ex, int spid)
	{
		String norm = normalize(sql);
		String hash = hash(norm);
		String key  = scenario + "|" + hash;

		// The full text, once per run
		if (_journal != null && _sqlSeen.add(key))
			_journal.event("sql", "scenario", scenario, "sqlHash", hash, "sqlText", norm, "example", sql.equals(norm) ? null : sql.trim());

		MinuteAgg agg = _minute.computeIfAbsent(key, k -> {
			MinuteAgg a = new MinuteAgg();
			a.scenario = scenario;
			a.sqlHash  = hash;
			return a;
		});
		agg.add(ms, rows, ex, spid);
	}

	/** Write the current minute's rows to the journal, and start a new minute */
	public synchronized void flushMinute()
	{
		Map<String, MinuteAgg> prev = _minute;
		OffsetDateTime prevStart = _minuteStart;
		_minute      = new ConcurrentHashMap<>();
		_minuteStart = OffsetDateTime.now().truncatedTo(ChronoUnit.MINUTES);

		if (_journal == null)
			return;
		for (MinuteAgg a : new TreeMap<>(prev).values())
		{
			synchronized (a)
			{
				_journal.event("minute",
						"minute",     prevStart.format(Journal.TS_FORMAT),
						"scenario",   a.scenario,
						"sqlHash",    a.sqlHash,
						"count",      a.count,
						"errors",     a.errors,
						"avgMs",      a.count == 0 ? 0 : a.sumMs / a.count,
						"maxMs",      a.maxMs,
						"rows",       a.rows,
						"spids",      a.spids,
						"errorCodes", a.errorCodes.isEmpty() ? null : a.errorCodes);
			}
		}
	}
}
