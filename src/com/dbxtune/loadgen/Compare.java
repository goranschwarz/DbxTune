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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The "compare" mode: what did DbxTune see of a load run?
 * <p>
 * Reads a run journal (what we did, and what DbxTune should show for each problem event) and asks DbxCentral (REST, read only)
 * what it did show: the alarm history ({@code /api/alarm/history}) and counter graphs ({@code /api/graph/data}).
 * Writes {@code <journal>.compare.md} next to the journal and a short summary on stdout.
 * <p>
 * Per problem event: each expected alarm is SEEN (raised in start .. end + grace, or already active then), LATE (up to 5 minutes
 * after that) or MISSED; each expected graph is SEEN if its peak in the event is at least {@code factor} x the run's normal level.
 * Alarms raised during the run that no problem expected are listed too ("what else did DbxTune notice").
 */
public class Compare
{
	private static final DateTimeFormatter CENTRAL_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final DateTimeFormatter HHMMSS       = DateTimeFormatter.ofPattern("HH:mm:ss");
	private static final long              LATE_MS      = 5 * 60_000;

	private final LoadGen      _lg;
	private final ObjectMapper _om = new ObjectMapper();
	private final HttpClient   _http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

	private String _centralUrl, _centralSrv, _token;
	private ZoneId _zone = ZoneId.systemDefault(); // the journal's zone (from its timestamps), used for the Central query parameters

	/** @param lg only its command line and properties are used (no DBMS connection) */
	public Compare(LoadGen lg)
	{
		_lg = lg;
	}

	//------------------------------------------------------------------
	// Data from the journal and from DbxCentral
	//------------------------------------------------------------------
	/** One alarm from raise to cancel (end = null: still active / no cancel seen) */
	private static class AlarmSpan
	{
		String alarmClass, cmName, extraInfo, severity, description;
		Instant start, end;
		boolean used; // matched by an expected alarm
	}

	/** One graph: sample time -> {series label -> value} */
	private static class Graph
	{
		TreeMap<Instant, Map<String, Double>> samples = new TreeMap<>();
	}

	//------------------------------------------------------------------
	// MAIN
	//------------------------------------------------------------------
	public int compare(File journalFile)
	throws Exception
	{
		//--- Journal
		List<Map<String, Object>> events = new ArrayList<>();
		for (String line : Files.readAllLines(journalFile.toPath(), StandardCharsets.UTF_8))
			if (!line.isBlank())
				events.add(_om.readValue(line, new TypeReference<Map<String, Object>>() {}));

		Map<String, Object> header = null, summary = null;
		List<Map<String, Object>> problems = new ArrayList<>();
		for (Map<String, Object> e : events)
		{
			String type = String.valueOf(e.get("type"));
			if      (type.equals("header") && header == null) header  = e;
			else if (type.equals("summary"))                  summary = e;
			else if (type.equals("problem"))                  problems.add(e);
		}
		if (header == null || !"run".equals(header.get("kind")))
		{
			System.out.println("ERROR: '" + journalFile + "' is not the journal of a 'run' (kind=" + (header == null ? "?" : header.get("kind")) + "). Compare works on run journals.");
			return LoadGen.EXIT_NOT_AVAILABLE;
		}
		// Journals written before 'graphs' existed in the expected lists: take alarms + graphs from the scenario's current definition
		Vendor vendor = Vendor.fromString(String.valueOf(header.get("vendor")));
		int graphsFromCode = 0, rowsFromCode = 0;
		for (Map<String, Object> p : problems)
		{
			Map<String, Object> exp = expected(p);
			boolean noGraphs = !exp.containsKey("graphs"), noRows = !exp.containsKey("rows");
			if (!(noGraphs || noRows) || vendor == null || !(p.get("expected") instanceof Map))
				continue;
			for (com.dbxtune.loadgen.scenario.Scenario sc : com.dbxtune.loadgen.scenario.Scenario.all())
			{
				Map<String, Object> now = sc.getName().equals(p.get("scenario")) ? sc.getExpected(vendor) : null;
				if (now == null)
					continue;
				if (noGraphs)
				{
					exp.put("alarms", now.get("alarms"));
					exp.put("graphs", now.get("graphs") == null ? Collections.emptyList() : now.get("graphs"));
					graphsFromCode++;
				}
				if (noRows)
				{
					exp.put("rows", now.get("rows") == null ? Collections.emptyList() : now.get("rows"));
					rowsFromCode++;
				}
			}
		}
		boolean doRows     = !_lg.getCmd().hasOption("no-rows");
		int     maxSamples = Integer.parseInt(prop("compare.rows.maxSamples", "8"));

		OffsetDateTime runStartOdt = OffsetDateTime.parse(String.valueOf(header.get("ts")));
		_zone = runStartOdt.getOffset();
		Instant runStart = runStartOdt.toInstant();
		Instant runEnd   = OffsetDateTime.parse(String.valueOf((summary != null ? summary : events.get(events.size() - 1)).get("ts"))).toInstant();
		long    graceMs  = LoadGen.parseDurationSec(opt("grace", prop("compare.grace", "120s")), null) * 1000L;
		double  factor   = Double.parseDouble(prop("compare.graph.factor", "2"));
		double  minDelta = Double.parseDouble(prop("compare.graph.minDelta", "0.5"));

		//--- DbxCentral
		String srvName = String.valueOf(header.get("server"));
		_centralUrl = opt("central", prop("central.url", null));
		_centralSrv = opt("central-srv", prop("central.srv." + srvName, prop("central.srv", null)));
		_token      = opt("central-token", prop("central.token", null));
		if (_centralUrl == null || _centralSrv == null)
		{
			System.out.println("ERROR: need the DbxCentral URL and DbxCentral's name for server '" + srvName + "':\n"
					+ "   --central http://host:8080 --central-srv <name as in DbxCentral, e.g. GS-2-WIN__SS_2025>\n"
					+ "   or in loadgen.properties: central.url=... and central.srv." + srvName + "=...");
			return LoadGen.EXIT_USAGE;
		}
		_centralUrl = _centralUrl.replaceAll("/+$", "");

		// Which graphs exist (also: is the server known, is Central there)
		Set<String> knownGraphs = new HashSet<>();
		String summaryGraph = null;
		try
		{
			for (Map<String, Object> g : getList("/api/graphs", "srv", _centralSrv))
			{
				String key = g.get("cmName") + "." + g.get("graphName");
				knownGraphs.add(key);
				if (summaryGraph == null && "CmSummary".equals(g.get("cmName")))
					summaryGraph = key;
			}
		}
		catch (IOException ex)
		{
			System.out.println("ERROR: can't read from DbxCentral '" + _centralUrl + "' (server '" + _centralSrv + "'): " + ex.getMessage());
			return LoadGen.EXIT_NOT_AVAILABLE;
		}
		if (knownGraphs.isEmpty())
		{
			System.out.println("ERROR: DbxCentral '" + _centralUrl + "' has no graphs for server '" + _centralSrv + "'. Is that DbxCentral's name for it? (see the server list on its start page)");
			return LoadGen.EXIT_NOT_AVAILABLE;
		}

		// Ask a wide window (the Central server's time zone may differ from the journal's), then filter on the exact instants
		Instant qFrom = runStart.minus(Duration.ofMinutes(70));
		Instant qTo   = runEnd.plus(Duration.ofMinutes(70));

		List<AlarmSpan> alarms = readAlarms(qFrom, qTo);

		Map<String, Graph> graphs = new HashMap<>();
		for (Map<String, Object> p : problems)
			for (String spec : listOf(expected(p).get("graphs")))
			{
				String key = spec.contains(":") ? spec.substring(0, spec.indexOf(':')) : spec;
				if (!graphs.containsKey(key) && knownGraphs.contains(key))
					graphs.put(key, readGraph(key, qFrom, qTo));
			}
		if (summaryGraph != null && !graphs.containsKey(summaryGraph))
			graphs.put(summaryGraph, readGraph(summaryGraph, qFrom, qTo));

		//--- Match each problem event
		List<String[]> rows = new ArrayList<>(); // time, scenario, sessions, check, result, detail
		Map<String, int[]> perScenario = new TreeMap<>(); // seen, late, missed, other
		Map<String, Set<String>> notChecked = new TreeMap<>();
		for (Map<String, Object> p : problems)
		{
			String  scenario = String.valueOf(p.get("scenario"));
			Instant start    = OffsetDateTime.parse(String.valueOf(p.get("startTs"))).toInstant();
			Instant end      = OffsetDateTime.parse(String.valueOf(p.get("endTs"))).toInstant();
			Instant winEnd   = end.plusMillis(graceMs);
			Instant nextSame = null; // the next event of the same scenario: a LATE alarm must start before it (else it is that one's alarm)
			for (Map<String, Object> q : problems)
			{
				Instant qs = OffsetDateTime.parse(String.valueOf(q.get("startTs"))).toInstant();
				if (scenario.equals(q.get("scenario")) && qs.isAfter(start) && (nextSame == null || qs.isBefore(nextSame)))
					nextSame = qs;
			}
			Map<String, Object> exp = expected(p);
			String sessions  = sessionsText(p);
			Map<String, String> spidRoles = spidRoles(p);
			int[] cnt = perScenario.computeIfAbsent(scenario, k -> new int[4]);
			boolean anyCheck = false;
			Set<String> covered = new HashSet<>(); // CMs this event's checks looked at

			if (p.get("error") != null)
				rows.add(new String[] {hhmmss(start), scenario, sessions, "(run)", "ERROR", String.valueOf(p.get("error"))});
			if (p.get("note") != null && String.valueOf(p.get("note")).startsWith("Skipped"))
			{
				rows.add(new String[] {hhmmss(start), scenario, sessions, "(run)", "SKIPPED", String.valueOf(p.get("note"))});
				continue;
			}

			// Alarms
			for (String alarm : listOf(exp.get("alarms")))
			{
				anyCheck = true;
				String cls = alarm.startsWith("AlarmEvent") ? alarm.substring("AlarmEvent".length()) : alarm;
				AlarmSpan inWindow = null, already = null, late = null, rightSession = null;
				for (AlarmSpan a : alarms) // in start order
				{
					if (!a.alarmClass.equals(cls))
						continue;
					// An alarm about one session (extraInfo = spid) belongs to this problem only if it is one of its sessions
					if (isSpid(a) && !spidRoles.isEmpty() && sessionMatch(a, spidRoles) == null)
						continue;
					boolean startsIn   = !a.start.isBefore(start) && !a.start.isAfter(winEnd);
					boolean activeAt   = a.start.isBefore(start) && (a.end == null || a.end.isAfter(start));
					boolean startsLate = a.start.isAfter(winEnd) && !a.start.isAfter(winEnd.plusMillis(LATE_MS)) && (nextSame == null || a.start.isBefore(nextSame));
					if (startsIn)
					{
						a.used = true; // all of them (e.g. the same alarm from CmSummary and CmActiveStatements) are explained by this problem
						covered.add(a.cmName);
						if (inWindow == null) inWindow = a;
						if (rightSession == null && sessionMatch(a, spidRoles) != null) rightSession = a;
					}
					if (activeAt && already == null) already = a;
					if (startsLate && late == null)  late = a;
				}
				for (AlarmSpan a : new AlarmSpan[] {inWindow, already, late})
					if (a != null) covered.add(a.cmName);
				String result, detail;
				if (inWindow != null)
				{
					result = "SEEN";
					detail = "raised " + hhmmss(inWindow.start) + " (+" + secs(start, inWindow.start) + " s) by " + inWindow.cmName
							+ (rightSession != null ? ", right session (" + sessionMatch(rightSession, spidRoles) + ") by " + rightSession.cmName + " at " + hhmmss(rightSession.start)
							                        : (inWindow.extraInfo.isEmpty() ? "" : ", " + inWindow.extraInfo));
				}
				else if (already != null)
				{
					already.used = true;
					result = "SEEN";
					detail = "already active since " + hhmmss(already.start) + " (" + already.cmName + ")";
				}
				else if (late != null)
				{
					late.used = true;
					result = "LATE";
					detail = "raised " + hhmmss(late.start) + ", " + secs(end, late.start) + " s after the problem ended";
				}
				else
				{
					result = "MISSED";
					detail = "no " + cls + " alarm from " + hhmmss(start) + " to " + hhmmss(winEnd.plusMillis(LATE_MS));
				}
				count(cnt, result);
				rows.add(new String[] {hhmmss(start), scenario, sessions, "alarm " + cls, result, detail});
			}

			// Graphs
			for (String spec : listOf(exp.get("graphs")))
			{
				anyCheck = true;
				String key    = spec.contains(":") ? spec.substring(0, spec.indexOf(':')) : spec;
				String series = spec.contains(":") ? spec.substring(spec.indexOf(':') + 1) : null;
				Graph  g      = graphs.get(key);
				String result, detail;
				if (g == null)
				{
					result = "NOT FOUND";
					detail = "graph " + key + " does not exist in DbxCentral for " + _centralSrv;
					cnt[3]++;
				}
				else
				{
					// Normal level: median over the run, outside this scenario's own events; peak: max in this event (+1 sample)
					List<Double> normal = new ArrayList<>();
					double peak = Double.NaN;
					Instant peakAt = null;
					for (Map.Entry<Instant, Map<String, Double>> s : g.samples.entrySet())
					{
						Double v = value(s.getValue(), series);
						if (v == null || s.getKey().isBefore(runStart) || s.getKey().isAfter(runEnd.plusMillis(graceMs)))
							continue;
						if (!s.getKey().isBefore(start) && !s.getKey().isAfter(end.plusSeconds(60)))
						{
							if (Double.isNaN(peak) || v > peak) { peak = v; peakAt = s.getKey(); }
						}
						else if (!insideScenario(s.getKey(), scenario, problems, 60))
							normal.add(v);
					}
					if (Double.isNaN(peak))
					{
						result = "NO DATA";
						detail = key + (series != null ? " [" + series + "]" : "") + ": no sample in " + hhmmss(start) + "-" + hhmmss(end.plusSeconds(60))
								+ (g.samples.isEmpty() ? "" : " (series names: " + g.samples.firstEntry().getValue().keySet() + ")");
						cnt[3]++;
					}
					else
					{
						double base = median(normal);
						boolean seen = peak >= base * factor && peak - base >= minDelta;
						result = seen ? "SEEN" : "MISSED";
						detail = key + (series != null ? " [" + series + "]" : "") + ": normal " + fmt(base) + " -> peak " + fmt(peak) + " at " + hhmmss(peakAt)
								+ (seen ? "" : " (needs >= " + fmt(Math.max(base * factor, base + minDelta)) + ")");
						count(cnt, result);
					}
				}
				rows.add(new String[] {hhmmss(start), scenario, sessions, "graph " + key.substring(key.indexOf('.') + 1) + (series != null ? ":" + series : ""), result, detail});
			}

			// Rows: did the CM have a row for this problem (its sessions, its SQL tag, a table)?
			for (String spec : doRows ? listOf(exp.get("rows")) : Collections.<String>emptyList())
			{
				anyCheck = true;
				String[] res = checkRows(spec, scenario, start, end, winEnd, spidRoles, maxSamples);
				if (!res[0].equals("NOT RECORDED"))
					covered.add(spec.substring(0, spec.indexOf(':')));
				count(cnt, res[0]);
				rows.add(new String[] {hhmmss(start), scenario, sessions, "rows " + spec.replace("@", ""), res[0], res[1]});
			}

			// The expected CMs that no check above covers
			for (String spec : listOf(exp.get("graphs")))
				covered.add(spec.substring(0, spec.indexOf('.')));
			for (String cm : listOf(exp.get("cms")))
				if (!covered.contains(cm))
					notChecked.computeIfAbsent(scenario, k -> new java.util.TreeSet<>()).add(cm);

			if (!anyCheck)
			{
				rows.add(new String[] {hhmmss(start), scenario, sessions, "-", "NO CHECK", "no alarm or graph expected for " + header.get("vendor") + " (see the note in the journal)"});
				cnt[3]++;
			}
		}

		//--- Alarms during the run that no problem expected
		Map<String, List<AlarmSpan>> unexpected = new TreeMap<>();
		for (AlarmSpan a : alarms)
			if (!a.used && !a.start.isBefore(runStart) && !a.start.isAfter(runEnd.plusMillis(graceMs)))
				unexpected.computeIfAbsent(a.alarmClass + " (" + a.cmName + ")", k -> new ArrayList<>()).add(a);

		//--- Sample spacing
		String spacing = "unknown (no CmSummary graph)";
		if (summaryGraph != null)
		{
			List<Double> gaps = new ArrayList<>();
			Instant prev = null;
			for (Instant t : graphs.get(summaryGraph).samples.keySet())
			{
				if (t.isBefore(runStart) || t.isAfter(runEnd)) continue;
				if (prev != null) gaps.add((t.toEpochMilli() - prev.toEpochMilli()) / 1000.0);
				prev = t;
			}
			if (!gaps.isEmpty())
			{
				Collections.sort(gaps);
				spacing = gaps.size() + 1 + " samples, seconds between them: median " + fmt(median(gaps)) + ", p90 " + fmt(gaps.get((int) (gaps.size() * 0.9)))
						+ ", max " + fmt(gaps.get(gaps.size() - 1)) + " (from " + summaryGraph + ")";
			}
		}

		//--- Report
		File out = new File(journalFile.getPath().replaceAll("\\.jsonl$", "") + ".compare.md");
		try (PrintWriter w = new PrintWriter(out, StandardCharsets.UTF_8))
		{
			Object info = header.get("installInfo");
			String size = info instanceof Map ? String.valueOf(((Map<?, ?>) info).get("size")) : "?";
			w.println("# Load run vs DbxTune: run " + header.get("runId") + " on " + srvName);
			w.println();
			w.println("| | |");
			w.println("|---|---|");
			w.println("| Run | " + header.get("runId") + ", " + runStartOdt.format(CENTRAL_TIME) + " - " + hhmmss(runEnd) + ", intensity " + header.get("intensity") + ", size " + size + " |");
			w.println("| DBMS | " + header.get("vendor") + " " + String.valueOf(header.get("dbmsVersion")).replace('\n', ' ') + " |");
			w.println("| DbxCentral | " + _centralUrl + ", server " + _centralSrv + " |");
			w.println("| Rules | alarm SEEN if raised from the problem start to its end + " + (graceMs / 1000) + " s (or already active), LATE if up to 5 min after that; graph SEEN if peak >= " + fmt(factor) + " x the run's normal level and >= normal + " + fmt(minDelta) + " |");
			w.println("| Samples | " + spacing + " |");
			if (graphsFromCode > 0)
				w.println("| Note | the journal was written by an older version (no expected graphs): " + graphsFromCode + " problem events use the expected alarms and graphs of the current scenario definitions |");
			if (rowsFromCode > 0 && doRows)
				w.println("| Note | the journal has no expected rows (older version): " + rowsFromCode + " problem events use the row checks of the current scenario definitions |");
			if (!doRows)
				w.println("| Note | row checks are off (--no-rows) |");
			w.println();
			w.println("## Summary per scenario");
			w.println();
			w.println("| Scenario | Events | SEEN | LATE | MISSED | other |");
			w.println("|---|---|---|---|---|---|");
			Map<String, Integer> eventCount = new TreeMap<>();
			for (Map<String, Object> p : problems) eventCount.merge(String.valueOf(p.get("scenario")), 1, Integer::sum);
			for (Map.Entry<String, int[]> e : perScenario.entrySet())
				w.println("| " + e.getKey() + " | " + eventCount.get(e.getKey()) + " | " + e.getValue()[0] + " | " + e.getValue()[1] + " | " + e.getValue()[2] + " | " + e.getValue()[3] + " |");
			w.println();
			w.println("## Problem events");
			w.println();
			w.println("| Time | Scenario | Sessions | Check | Result | Detail |");
			w.println("|---|---|---|---|---|---|");
			for (String[] r : rows)
				w.println("| " + String.join(" | ", Arrays.stream(r).map(c -> c.replace("|", "/")).toArray(String[]::new)) + " |");
			w.println();
			w.println("## Alarms nobody expected (raised during the run)");
			w.println();
			if (unexpected.isEmpty())
				w.println("None.");
			else
			{
				w.println("| Alarm (CM) | Count | Times | Example |");
				w.println("|---|---|---|---|");
				for (Map.Entry<String, List<AlarmSpan>> e : unexpected.entrySet())
				{
					List<String> times = new ArrayList<>();
					for (AlarmSpan a : e.getValue()) times.add(hhmmss(a.start) + (a.extraInfo.isEmpty() ? "" : " " + a.extraInfo));
					w.println("| " + e.getKey() + " | " + e.getValue().size() + " | " + String.join(", ", times) + " | " + e.getValue().get(0).description.replace("|", "/").replace("\n", " ") + " |");
				}
			}
			w.println();
			w.println("## Expected CMs that this compare does not check (yet)");
			w.println();
			w.println("Each needs a look in the recording (or a row-level check later):");
			w.println();
			for (Map.Entry<String, Set<String>> e : notChecked.entrySet())
				w.println("- " + e.getKey() + ": " + String.join(", ", e.getValue()));
		}

		//--- stdout
		System.out.println("Run " + header.get("runId") + " on " + srvName + " (" + header.get("intensity") + ") vs DbxCentral " + _centralSrv + ":");
		System.out.println(String.format(Locale.US, "   %-16s %6s %5s %5s %7s %6s", "scenario", "events", "SEEN", "LATE", "MISSED", "other"));
		Map<String, Integer> eventCount = new TreeMap<>();
		for (Map<String, Object> p : problems) eventCount.merge(String.valueOf(p.get("scenario")), 1, Integer::sum);
		for (Map.Entry<String, int[]> e : perScenario.entrySet())
			System.out.println(String.format(Locale.US, "   %-16s %6d %5d %5d %7d %6d", e.getKey(), eventCount.get(e.getKey()), e.getValue()[0], e.getValue()[1], e.getValue()[2], e.getValue()[3]));
		if (!unexpected.isEmpty())
		{
			System.out.println("   Alarms nobody expected:");
			for (Map.Entry<String, List<AlarmSpan>> e : unexpected.entrySet())
				System.out.println("      " + e.getValue().size() + " x " + e.getKey());
		}
		System.out.println("   Samples: " + spacing);
		System.out.println("Report: " + out.getAbsolutePath());
		return LoadGen.EXIT_OK;
	}

	//------------------------------------------------------------------
	// DbxCentral
	//------------------------------------------------------------------
	private List<AlarmSpan> readAlarms(Instant from, Instant to)
	throws IOException
	{
		List<Map<String, Object>> rows = getList("/api/alarm/history", "srv", _centralSrv, "startTime", central(from), "endTime", central(to));
		// Raise ... cancel, per alarm (class + CM + extraInfo); RE-RAISE keeps it open
		Map<String, AlarmSpan> open = new HashMap<>();
		List<AlarmSpan> spans = new ArrayList<>();
		rows.sort((a, b) -> String.valueOf(a.get("eventTime")).compareTo(String.valueOf(b.get("eventTime"))));
		for (Map<String, Object> r : rows)
		{
			String  action = String.valueOf(r.get("action"));
			String  key    = r.get("alarmClass") + "|" + r.get("serviceInfo") + "|" + r.get("extraInfo");
			Instant t      = OffsetDateTime.parse(String.valueOf(r.get("eventTime"))).toInstant();
			if (action.equals("RAISE") || (action.equals("RE-RAISE") && !open.containsKey(key)))
			{
				AlarmSpan a = new AlarmSpan();
				a.alarmClass  = String.valueOf(r.get("alarmClass"));
				a.cmName      = String.valueOf(r.get("serviceInfo"));
				a.extraInfo   = r.get("extraInfo") == null ? "" : String.valueOf(r.get("extraInfo")).trim();
				a.severity    = String.valueOf(r.get("severity"));
				a.description = String.valueOf(r.get("description"));
				a.start       = t;
				open.put(key, a);
				spans.add(a);
			}
			else if (action.equals("CANCEL"))
			{
				AlarmSpan a = open.remove(key);
				if (a != null)
					a.end = t;
			}
		}
		return spans;
	}

	private static final DateTimeFormatter SAMPLE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSS]");
	private static final Pattern           ROWS_SPEC   = Pattern.compile("^(\\w+):([\\w ]+)([=~])(.*)$");

	/**
	 * One row check, e.g. "CmOpenTransactions:session_id=@sessions": walk the CM's recorded samples from the problem start
	 * to the end of its window (at most maxSamples), stop at the first sample with a matching row.
	 * @return {result, detail}: SEEN / MISSED / NOT RECORDED / COLUMN NOT FOUND
	 */
	private String[] checkRows(String spec, String scenario, Instant start, Instant end, Instant winEnd, Map<String, String> spidRoles, int maxSamples)
	{
		Matcher m = ROWS_SPEC.matcher(spec);
		if (!m.matches())
			return new String[] {"BAD SPEC", "can't parse '" + spec + "', expected CmName:column=value or CmName:column~value"};
		String  cm = m.group(1), col = m.group(2), op = m.group(3), val = m.group(4);
		boolean bySession = val.equals("@sessions");
		String  want = val.equals("@tag") ? "dbxt:" + scenario : val;
		if (bySession && spidRoles.isEmpty())
			return new String[] {"NO SESSIONS", "the journal has no sessions for this event"};

		// Sessions: only until shortly after the problem ended (a spid is reused soon after).
		// Statements / tables: CMs like CmExecQueryStats are refreshed every few minutes and their content is cumulative,
		// so the first sample up to 'compare.rows.lookAheadMinutes' after the window still counts.
		long    lookAheadMs = Long.parseLong(prop("compare.rows.lookAheadMinutes", "60")) * 60_000L;
		Instant limit = bySession ? end.plusSeconds(60) : winEnd.plusMillis(lookAheadMs);
		String  from = central(start);
		int     checked = 0;
		Instant first = null, last = null;
		try
		{
			for (int i = 0; i < maxSamples; i++)
			{
				Map<String, Object> nav = getObject("/api/cc/mgt/cm/navSample", "srv", _centralSrv, "cm", cm, "time", from, "dir", "next");
				if (!Boolean.TRUE.equals(nav.get("found")))
					break;
				String  sampleTime = String.valueOf(nav.get("sampleTime"));
				Instant t = java.time.LocalDateTime.parse(sampleTime, SAMPLE_TIME).atZone(_zone).toInstant();
				if (t.isAfter(limit) || (!bySession && t.isAfter(winEnd) && checked > 0))
					break; // statements/tables: at most one sample after the window
				from = sampleTime;
				checked++;
				if (first == null) first = t;
				last = t;

				Map<String, Object> data = getObject("/api/cc/mgt/cm/data", "srv", _centralSrv, "cm", cm, "time", sampleTime, "type", "abs");
				List<String> cols = listOf(data.get("columns"));
				if (cols.isEmpty())
					continue; // no rows in this sample
				int ci = cols.indexOf(col);
				if (ci < 0)
					return new String[] {"COLUMN NOT FOUND", cm + " has no column '" + col + "'. Columns: " + String.join(", ", cols)};
				int ctxi = cols.indexOf("context_info_str"), waiti = cols.indexOf("wait_type");

				List<String> hits = new ArrayList<>();
				Set<String>  ctx  = new java.util.TreeSet<>();
				int matches = 0;
				for (Object r : data.get("rows") instanceof List ? (List<?>) data.get("rows") : Collections.emptyList())
				{
					if (!(r instanceof List) || ((List<?>) r).size() <= ci)
						continue;
					List<?> row  = (List<?>) r;
					String  cell = String.valueOf(row.get(ci)).trim().replaceAll("\\.0$", "");
					boolean hit  = bySession ? spidRoles.containsKey(cell)
					             : op.equals("=") ? cell.equalsIgnoreCase(want) : cell.toLowerCase().contains(want.toLowerCase());
					// A reused spid: the session now does another scenario (context_info dbxt:<other>), not this problem's session
					if (hit && bySession && ctxi >= 0)
					{
						String c = String.valueOf(row.get(ctxi)).trim();
						if (c.startsWith("dbxt:") && !c.startsWith("dbxt:" + scenario))
							hit = false;
					}
					if (!hit)
						continue;
					matches++;
					if (bySession)
					{
						String role = spidRoles.get(cell);
						Object wait = waiti >= 0 ? row.get(waiti) : null;
						hits.add((role.length() <= 12 ? role + " " : "") + cell + (wait != null && !String.valueOf(wait).isBlank() && !"null".equals(String.valueOf(wait)) ? " (" + wait + ")" : ""));
					}
					else if (hits.isEmpty())
					{
						String one = cell.replaceAll("\\s+", " ");
						hits.add("'" + one.substring(0, Math.min(90, one.length())) + "'");
					}
					if (ctxi >= 0 && row.get(ctxi) != null && !String.valueOf(row.get(ctxi)).isBlank() && !"null".equals(String.valueOf(row.get(ctxi))))
						ctx.add(String.valueOf(row.get(ctxi)).trim());
				}
				if (matches > 0)
					return new String[] {"SEEN", cm + " at " + hhmmss(t) + (t.isAfter(winEnd) ? " (first sample after the window)" : " (sample " + checked + ")") + ": "
							+ (bySession ? String.join(", ", hits) : matches + " row(s) with " + col + (op.equals("=") ? " = " : " containing ") + "'" + want + "', e.g. " + hits.get(0))
							+ (ctx.isEmpty() ? "" : "; context_info " + String.join(", ", ctx))};
			}
		}
		catch (IOException ex)
		{
			return new String[] {"NOT RECORDED", cm + ": " + ex.getMessage()};
		}
		if (checked == 0)
			return new String[] {"NOT RECORDED", cm + " has no sample from " + hhmmss(start) + " to " + hhmmss(limit) + (bySession ? " (the problem was shorter than the time between samples, or the CM is postponed)" : " (not in this collector, or postponed longer)")};
		if (bySession && first != null && first.isAfter(end))
			return new String[] {"TOO SHORT", "the problem (" + hhmmss(start) + "-" + hhmmss(end) + ") ended before the next " + cm + " sample (" + hhmmss(first) + "): it can't be in any sample"};
		return new String[] {"MISSED", "no " + cm + " row with " + col + (op.equals("=") ? " = " : " containing ") + (bySession ? "one of " + spidRoles.keySet() : "'" + want + "'")
				+ " in " + checked + " sample(s) " + hhmmss(first) + "-" + hhmmss(last)};
	}

	private Graph readGraph(String key, Instant from, Instant to)
	throws IOException
	{
		Graph g = new Graph();
		String cm = key.substring(0, key.indexOf('.')), name = key.substring(key.indexOf('.') + 1);
		for (Map<String, Object> r : getList("/api/graph/data", "srv", _centralSrv, "cmName", cm, "graphName", name, "startTime", central(from), "endTime", central(to), "sampleType", "ALL"))
		{
			Map<String, Double> data = new LinkedHashMap<>();
			Object d = r.get("data");
			if (d instanceof Map)
				for (Map.Entry<?, ?> e : ((Map<?, ?>) d).entrySet())
					if (e.getValue() instanceof Number)
						data.put(String.valueOf(e.getKey()), ((Number) e.getValue()).doubleValue());
			g.samples.put(OffsetDateTime.parse(String.valueOf(r.get("sessionSampleTime"))).toInstant(), data);
		}
		return g;
	}

	private List<Map<String, Object>> getList(String path, String... params)
	throws IOException
	{
		String body = getBody(path, params);
		if (!body.startsWith("["))
			throw new IOException("Unexpected answer from " + path + ": " + body.substring(0, Math.min(300, body.length())));
		return _om.readValue(body, new TypeReference<List<Map<String, Object>>>() {});
	}

	private Map<String, Object> getObject(String path, String... params)
	throws IOException
	{
		String body = getBody(path, params);
		if (!body.startsWith("{"))
			throw new IOException("Unexpected answer from " + path + ": " + body.substring(0, Math.min(300, body.length())));
		Map<String, Object> m = _om.readValue(body, new TypeReference<Map<String, Object>>() {});
		if (m.get("error") != null && !"no-data-in-window".equals(m.get("error")))
			throw new IOException(m.get("error") + ": " + m.get("message"));
		return m;
	}

	private String getBody(String path, String... params)
	throws IOException
	{
		StringBuilder url = new StringBuilder(_centralUrl).append(path);
		for (int i = 0; i < params.length; i += 2)
			url.append(i == 0 ? '?' : '&').append(params[i]).append('=').append(URLEncoder.encode(params[i + 1], StandardCharsets.UTF_8));
		HttpRequest.Builder rb = HttpRequest.newBuilder(URI.create(url.toString())).timeout(Duration.ofSeconds(60)).GET();
		if (_token != null)
			rb.header("Authorization", "Bearer " + _token);
		try
		{
			HttpResponse<String> resp = _http.send(rb.build(), HttpResponse.BodyHandlers.ofString());
			if (resp.statusCode() != 200)
			{
				// Jetty error pages: the message is in <title>
				String msg = resp.body();
				Matcher t = Pattern.compile("(?is)<title>(.*?)</title>").matcher(msg);
				msg = t.find() ? t.group(1).replace("&apos;", "'").replace("&quot;", "\"") : msg.substring(0, Math.min(300, msg.length())).trim();
				throw new IOException("HTTP " + resp.statusCode() + " from " + path + ": " + msg
						+ (resp.statusCode() == 401 ? " (DbxCentral wants a login: use --central-token <DbxTune.central.registration.token>)" : ""));
			}
			return resp.body().trim();
		}
		catch (java.net.ConnectException ex)
		{
			throw new IOException("can't connect to " + _centralUrl + " (" + ex.getClass().getSimpleName() + "). Is DbxCentral running there, and is the port right?", ex);
		}
		catch (InterruptedException ex)
		{
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted", ex);
		}
	}

	//------------------------------------------------------------------
	// Helpers
	//------------------------------------------------------------------
	private String opt(String name, String def)   { return _lg.getCmd().getOptionValue(name, def); }
	private String prop(String key, String def)   { return _lg.getProp(key, def); }

	private String central(Instant t) { return CENTRAL_TIME.format(t.atZone(_zone)); }
	private String hhmmss(Instant t)  { return t == null ? "-" : HHMMSS.format(t.atZone(_zone)); }
	private static long secs(Instant a, Instant b) { return Math.round((b.toEpochMilli() - a.toEpochMilli()) / 1000.0); }

	@SuppressWarnings("unchecked")
	private static Map<String, Object> expected(Map<String, Object> p)
	{
		Object e = p.get("expected");
		return e instanceof Map ? (Map<String, Object>) e : Collections.emptyMap();
	}

	private static List<String> listOf(Object o)
	{
		List<String> l = new ArrayList<>();
		if (o instanceof List)
			for (Object x : (List<?>) o) l.add(String.valueOf(x));
		return l;
	}

	private static String sessionsText(Map<String, Object> p)
	{
		List<String> l = new ArrayList<>();
		if (p.get("sessions") instanceof List)
			for (Object s : (List<?>) p.get("sessions"))
				if (s instanceof Map)
				{
					String role = String.valueOf(((Map<?, ?>) s).get("role"));
					l.add(((Map<?, ?>) s).get("spid") + (role.length() <= 12 ? " " + role : ""));
				}
		return l.size() > 4 ? l.size() + " sessions" : String.join(", ", l);
	}

	/** spid -> role, for this problem's sessions */
	private static Map<String, String> spidRoles(Map<String, Object> p)
	{
		Map<String, String> m = new HashMap<>();
		if (p.get("sessions") instanceof List)
			for (Object s : (List<?>) p.get("sessions"))
				if (s instanceof Map)
					m.put(String.valueOf(((Map<?, ?>) s).get("spid")), String.valueOf(((Map<?, ?>) s).get("role")));
		return m;
	}

	private static final Pattern SPID_IN_EXTRA = Pattern.compile("^(?:spid=)?(\\d+)$");

	private static boolean isSpid(AlarmSpan a) { return SPID_IN_EXTRA.matcher(a.extraInfo).matches(); }

	/** "holder 57" if the alarm's extraInfo is one of the problem's sessions (spid=57, or just 57), else null */
	private static String sessionMatch(AlarmSpan a, Map<String, String> spidRoles)
	{
		if (a == null)
			return null;
		Matcher m = SPID_IN_EXTRA.matcher(a.extraInfo);
		if (m.matches() && spidRoles.containsKey(m.group(1)))
			return spidRoles.get(m.group(1)) + " " + m.group(1);
		return null;
	}

	/** Value of one series, the sum of all ("*"), or the first series (null) */
	private static Double value(Map<String, Double> data, String series)
	{
		if (data.isEmpty())
			return null;
		if (series == null)
			return data.values().iterator().next();
		if (series.equals("*"))
			return data.values().stream().mapToDouble(Double::doubleValue).sum();
		return data.get(series);
	}

	/** Is t inside (or within padSec after) an event of the given scenario? */
	private static boolean insideScenario(Instant t, String scenario, List<Map<String, Object>> problems, int padSec)
	{
		for (Map<String, Object> p : problems)
		{
			if (!scenario.equals(p.get("scenario")))
				continue;
			Instant s = OffsetDateTime.parse(String.valueOf(p.get("startTs"))).toInstant();
			Instant e = OffsetDateTime.parse(String.valueOf(p.get("endTs"))).toInstant().plusSeconds(padSec);
			if (!t.isBefore(s) && !t.isAfter(e))
				return true;
		}
		return false;
	}

	private static double median(List<Double> l)
	{
		if (l.isEmpty())
			return 0;
		List<Double> c = new ArrayList<>(l);
		Collections.sort(c);
		return c.get(c.size() / 2);
	}

	private static String fmt(double d)
	{
		return d >= 100 ? String.format(Locale.US, "%.0f", d) : String.format(Locale.US, "%.1f", d);
	}

	private static void count(int[] cnt, String result)
	{
		switch (result)
		{
		case "SEEN":   cnt[0]++; break;
		case "LATE":   cnt[1]++; break;
		case "MISSED": cnt[2]++; break;
		default:       cnt[3]++; break;
		}
	}
}
