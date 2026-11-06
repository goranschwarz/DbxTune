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

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.AppDir;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The activity journal: one JSON object per line, telling WHAT the load generator did and WHEN.
 * <p>
 * Meant to be read next to a DbxTune recording of the same time window (by a person or an LLM),
 * to find out what the collectors saw, and what they missed.
 * <p>
 * Event types written:
 * <ul>
 *   <li><code>header</code>   - run/install parameters, server, DBMS version</li>
 *   <li><code>session</code>  - connect/disconnect of a connection: login, program name, the server's session id</li>
 *   <li><code>sql</code>      - the full text of a statement, once per run (key = scenario + sqlHash)</li>
 *   <li><code>minute</code>   - per minute and scenario+statement: count, errors, avg/max ms, rows, session ids</li>
 *   <li><code>problem</code>  - every problem scenario event in full, with an "expected" block (what DbxTune should show)</li>
 *   <li><code>object</code>   - install/uninstall: objects created/dropped, and the deliberate flaws ("answer key")</li>
 *   <li><code>info</code>/<code>error</code> - free text</li>
 *   <li><code>summary</code>  - the end of run totals</li>
 * </ul>
 */
public class Journal
implements Closeable
{
	private static final Logger _logger = LogManager.getLogger();

	public static final DateTimeFormatter TS_FORMAT   = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
	public static final DateTimeFormatter FILE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");

	private final ObjectMapper   _om = new ObjectMapper();
	private final File           _file;
	private final BufferedWriter _writer;

	/**
	 * Open a new journal file: ${journalDir}/{srvName}/{yyyy-MM-dd_HHmmss}_{kind}_{runId}.jsonl
	 */
	public Journal(String srvName, String kind, String runId)
	throws IOException
	{
		File dir = new File(getJournalDir(), srvName);
		dir.mkdirs();

		_file   = new File(dir, OffsetDateTime.now().format(FILE_FORMAT) + "_" + kind + "_" + runId + ".jsonl");
		_writer = Files.newBufferedWriter(_file.toPath(), StandardCharsets.UTF_8);
		_logger.info("Journal file: " + _file.getAbsolutePath());
	}

	public File getFile() { return _file; }

	/** ~/.dbxtune/loadgen/journal */
	public static File getJournalDir()
	{
		return new File(AppDir.getDbxUserHomeDir(true) + "loadgen" + File.separator + "journal");
	}

	public static String now()
	{
		return OffsetDateTime.now().format(TS_FORMAT);
	}

	/** A time in milliseconds, formatted like all other journal timestamps (local time with offset) */
	public static String ts(long timeMs)
	{
		return OffsetDateTime.ofInstant(java.time.Instant.ofEpochMilli(timeMs), java.time.ZoneId.systemDefault()).format(TS_FORMAT);
	}

	/**
	 * Write one event.
	 * @param type    the event type
	 * @param keyVals key, value, key, value... (a value may be a Map/List, it's written as JSON)
	 */
	public void event(String type, Object... keyVals)
	{
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("ts",   now());
		map.put("type", type);
		for (int i=0; i+1<keyVals.length; i+=2)
			map.put(String.valueOf(keyVals[i]), keyVals[i+1]);
		write(map);
	}

	/** Write a prepared map (a "ts" and "type" entry should already be in it) */
	public synchronized void write(Map<String, Object> map)
	{
		try
		{
			_writer.write(_om.writeValueAsString(map));
			_writer.write("\n");
			_writer.flush();
		}
		catch (IOException ex)
		{
			_logger.warn("Problems writing to journal '" + _file + "'. Caught: " + ex);
		}
	}

	@Override
	public synchronized void close()
	{
		try { _writer.close(); }
		catch (IOException ignore) {}
	}

	/** Remove journal files (all servers) older than 'keepDays' */
	public static void removeOldFiles(int keepDays)
	{
		if (keepDays <= 0)
			return;
		long limit = System.currentTimeMillis() - keepDays * 24L * 3600L * 1000L;
		File[] srvDirs = getJournalDir().listFiles(File::isDirectory);
		if (srvDirs == null)
			return;
		for (File srvDir : srvDirs)
		{
			File[] files = srvDir.listFiles((d, n) -> n.endsWith(".jsonl"));
			if (files == null)
				continue;
			for (File f : files)
			{
				if (f.lastModified() < limit && f.delete())
					_logger.info("Removed old journal file: " + f);
			}
		}
	}

	/** @return the newest journal file for a server (or all servers if srvName is null), null if none */
	public static File findLast(String srvName, String kind)
	{
		List<File> all = new ArrayList<>();
		File[] srvDirs = getJournalDir().listFiles(File::isDirectory);
		if (srvDirs != null)
		{
			for (File srvDir : srvDirs)
			{
				if (srvName != null && !srvDir.getName().equals(srvName))
					continue;
				File[] files = srvDir.listFiles((d, n) -> n.endsWith(".jsonl") && (kind == null || n.contains("_" + kind + "_")));
				if (files != null)
					all.addAll(Arrays.asList(files));
			}
		}
		return all.stream().max(Comparator.comparingLong(File::lastModified)).orElse(null);
	}

	/**
	 * Print a journal file in readable form: the header, the problem events as a time line and the summary.
	 */
	public static void print(File file, PrintStream out)
	throws IOException
	{
		ObjectMapper om = new ObjectMapper();
		out.println("Journal: " + file.getAbsolutePath());
		out.println();

		int sessions = 0, minutes = 0, sqls = 0;
		for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8))
		{
			if (line.isBlank())
				continue;
			JsonNode n = om.readTree(line);
			String type = n.path("type").asText();
			String ts   = n.path("ts").asText();
			String hhmmss = ts.length() >= 19 ? ts.substring(11, 19) : ts;

			switch (type)
			{
			case "header":
				out.println("== " + n.path("kind").asText() + " on " + n.path("server").asText() + " (" + n.path("vendor").asText() + ") started " + ts);
				n.fields().forEachRemaining(e -> {
					if (!Arrays.asList("ts", "type", "kind", "server", "vendor").contains(e.getKey()))
						out.println("   " + e.getKey() + " = " + e.getValue());
				});
				out.println();
				break;

			case "problem":
				out.println(hhmmss + "  PROBLEM " + n.path("scenario").asText()
						+ "  start=" + n.path("startTs").asText().substring(11, 19)
						+ " end="    + n.path("endTs"  ).asText().substring(11, 19)
						+ "  sessions=" + n.path("sessions")
						+ (n.hasNonNull("error") ? "  error=" + n.path("error").asText() : ""));
				if (n.hasNonNull("note"))
					out.println("            " + n.path("note").asText());
				out.println("            expected: " + n.path("expected"));
				break;

			case "object":
				out.println(hhmmss + "  " + n.path("action").asText() + " " + n.path("objectType").asText() + " " + n.path("name").asText()
						+ (n.hasNonNull("flaw") ? "   FLAW: " + n.path("flaw").asText() : ""));
				break;

			case "info":
			case "error":
				out.println(hhmmss + "  " + type.toUpperCase() + ": " + n.path("msg").asText());
				break;

			case "summary":
				out.println();
				out.println("== Summary " + ts);
				for (JsonNode s : n.path("scenarios"))
					out.println(String.format("   %-22s count=%-8d errors=%-6d avgMs=%-8s p95Ms=%-8s maxMs=%s",
							s.path("scenario").asText(), s.path("count").asLong(), s.path("errors").asLong(),
							s.path("avgMs").asText(), s.path("p95Ms").asText(), s.path("maxMs").asText()));
				n.fields().forEachRemaining(e -> {
					if (!Arrays.asList("ts", "type", "scenarios").contains(e.getKey()))
						out.println("   " + e.getKey() + " = " + e.getValue());
				});
				break;

			case "session": sessions++; break;
			case "minute":  minutes++;  break;
			case "sql":     sqls++;     break;
			}
		}
		out.println();
		out.println("(also in the file: " + sessions + " session events, " + sqls + " statement texts, " + minutes + " per-minute rows)");
	}
}
