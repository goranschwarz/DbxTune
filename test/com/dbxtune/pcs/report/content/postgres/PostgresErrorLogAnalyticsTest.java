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
package com.dbxtune.pcs.report.content.postgres;

import java.io.StringWriter;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Arrays;

import com.dbxtune.CounterController;
import com.dbxtune.CounterControllerNoOp;
import com.dbxtune.cloud.CloudLogEntry;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.postgres.CmErrorLog;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportDefault;
import com.dbxtune.pcs.report.content.IReportEntry.MessageType;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

/**
 * Self-contained unit &amp; H2 integration tests for Phase 2:
 * {@link PostgresErrorLogBgParser} and {@link PostgresErrorLog} operational analytics.
 */
public class PostgresErrorLogAnalyticsTest
{
	private static void assertTrue(boolean cond, String msg)
	{
		if (!cond)
			throw new AssertionError("ASSERTION FAILED: " + msg);
	}

	private static void assertEquals(Object expected, Object actual, String msg)
	{
		if (expected == null && actual == null)
			return;
		if (expected == null || !expected.equals(actual))
			throw new AssertionError("ASSERTION FAILED: " + msg + " | expected=<" + expected + "> actual=<" + actual + ">");
	}

	public static void testBgParserAllCategories()
	{
		PostgresErrorLogBgParser parser = new PostgresErrorLogBgParser();
		Timestamp t1 = Timestamp.valueOf("2026-11-18 10:00:00");
		Timestamp t2 = Timestamp.valueOf("2026-11-18 10:05:00");
		Timestamp t3 = Timestamp.valueOf("2026-11-18 10:10:00");

		// 1. Checkpoints (timed + wal + PG 17 slru buffers format)
		parser.parseLogRow(t1, null, null, 100, null, "checkpoint starting: time", null, null);
		parser.parseLogRow(t1, null, null, 100, null,
				"checkpoint complete: wrote 1024 buffers (6.2%); 0 WAL file(s) added, 1 removed, 2 recycled; "
				+ "write=10.500 s, sync=0.050 s, total=10.600 s; sync files=15, longest=0.020 s, average=0.003 s; "
				+ "distance=32768 kB, estimate=32768 kB; lsn=0/1A000028, redo lsn=0/18000060",
				null, null);

		parser.parseLogRow(t2, null, null, 100, null, "checkpoint starting: wal", null, null);
		parser.parseLogRow(t2, null, null, 100, null,
				"checkpoint complete: wrote 4096 buffers (25.0%), wrote 4 slru buffers; 2 WAL file(s) added, 0 removed, 5 recycled; "
				+ "write=45.200 s, sync=1.450 s, total=46.800 s; sync files=40, longest=1.250 s, average=0.036 s; "
				+ "distance=131072 kB, estimate=131072 kB; lsn=0/22000028, redo lsn=0/20000060",
				null, null);

		ResultSetTableModel ckptRstm = parser.buildCheckpointsRstm();
		assertEquals(2, ckptRstm.getRowCount(), "Should have 2 checkpoint reason groups (time and wal)");

		// 2. Autovacuum & Autoanalyze
		parser.parseLogRow(t2, null, null, 201, null,
				"automatic aggressive vacuum to prevent wraparound of table \"prod.public.orders\": index scans: 2\n"
				+ "pages: 10 removed, 5000 remain, 4200 scanned (84.00% of total)\n"
				+ "tuples: 120000 removed, 980000 remain, 250000 are dead but not yet removable, oldest xmin: 887766\n"
				+ "buffer usage: 8400 hits, 1600 misses, 900 dirtied\n"
				+ "avg read rate: 12.500 MB/s, avg write rate: 7.200 MB/s\n"
				+ "WAL usage: 4500 records, 300 full page images, 2097152 bytes\n"
				+ "system usage: CPU: user: 1.20 s, system: 0.30 s, elapsed: 14.50 s",
				null, null);

		parser.parseLogRow(t3, null, null, 202, null,
				"automatic analyze of table \"prod.public.orders\"\n"
				+ "buffer usage: 600 hits, 50 misses, 5 dirtied\n"
				+ "avg read rate: 4.500 MB/s, avg write rate: 0.400 MB/s\n"
				+ "system usage: CPU: user: 0.10 s, system: 0.05 s, elapsed: 0.50 s",
				null, null);

		ResultSetTableModel vacRstm = parser.buildAutovacuumRstm(50);
		assertEquals(1, vacRstm.getRowCount(), "Both vacuum and analyze on prod.public.orders should aggregate into 1 row");
		assertEquals("prod.public.orders", vacRstm.getValueAsString(0, "table_name"), "table_name");
		assertEquals(1, vacRstm.getValueAsInteger(0, "vacuum_count"), "vacuum_count");
		assertEquals(1, vacRstm.getValueAsInteger(0, "wraparound_count"), "wraparound_count");
		assertEquals(1, vacRstm.getValueAsInteger(0, "analyze_count"), "analyze_count");
		assertEquals(120000L, vacRstm.getValueAsLong(0, "tuples_removed__sum"), "tuples_removed__sum");
		assertEquals(250000L, vacRstm.getValueAsLong(0, "dead_not_removable__max"), "dead_not_removable__max");
		assertEquals(15.0, vacRstm.getValueAsDouble(0, "elapsed_sec__sum"), "elapsed_sec__sum (14.50 + 0.50)");

		// 3. Temporary files
		parser.parseLogRow(t2, "app_user", "prod", 301, "billing-svc",
				"temporary file: path \"base/pgsql_tmp/pgsql_tmp301.0\", size 52428800",
				null, "SELECT customer_id, sum(amount) FROM invoices WHERE region_id = 42 GROUP BY 1 ORDER BY 2 DESC");
		parser.parseLogRow(t3, "app_user", "prod", 301, "billing-svc",
				"temporary file: path \"base/pgsql_tmp/pgsql_tmp301.1\", size 104857600",
				null, "SELECT customer_id, sum(amount) FROM invoices WHERE region_id = 99 GROUP BY 1 ORDER BY 2 DESC");

		ResultSetTableModel tempRstm = parser.buildTempFilesRstm(50);
		assertEquals(1, tempRstm.getRowCount(), "Normalized temp file queries should group into 1 row");
		assertEquals(2, tempRstm.getValueAsInteger(0, "temp_file_count"), "temp_file_count");
		assertEquals(150.0, tempRstm.getValueAsDouble(0, "size_mb__sum"), "size_mb__sum (50 + 100 MB)");
		assertEquals(100.0, tempRstm.getValueAsDouble(0, "size_mb__max"), "size_mb__max (100 MB)");

		// 4. Lock waits
		parser.parseLogRow(t2, "app_user", "prod", 401, "order-api",
				"process 401 still waiting for ShareLock on transaction 998877 after 1002.500 ms",
				"Process holding the lock: 400. Wait queue: 401.",
				"UPDATE orders SET status = 'PAID' WHERE order_id = 1001");
		parser.parseLogRow(t3, "app_user", "prod", 401, "order-api",
				"process 401 acquired ShareLock on transaction 998877 after 3500.250 ms",
				null,
				"UPDATE orders SET status = 'PAID' WHERE order_id = 1001");

		ResultSetTableModel lockSumRstm  = parser.buildLockWaitsSummaryRstm();
		ResultSetTableModel lockRowsRstm = parser.buildLockWaitDetailsRstm(25);
		assertEquals(2, lockSumRstm.getRowCount(), "Should have 'still waiting' and 'acquired' groups");
		assertEquals(2, lockRowsRstm.getRowCount(), "Should have 2 individual lock wait detail rows");
		assertEquals(3500.3, lockRowsRstm.getValueAsDouble(0, "wait_ms"), "Longest lock wait should be sorted first");

		// 5. Slow queries & auto_explain
		parser.parseLogRow(t2, "report_user", "prod", 501, "metabase",
				"duration: 2500.400 ms  statement: SELECT count(*) FROM events WHERE created_at > '2026-01-01'",
				null, null);
		parser.parseLogRow(t3, "report_user", "prod", 501, "metabase",
				"duration: 3499.600 ms  statement: SELECT count(*) FROM events WHERE created_at > '2026-02-01'",
				null, null);
		parser.parseLogRow(t3, "app_user", "prod", 502, "order-api",
				"duration: 1800.000 ms  plan:\nQuery Text: SELECT * FROM orders WHERE customer_id = 123\nSeq Scan on orders  (cost=0.00..1250.00 rows=10 width=64)",
				null, null);

		ResultSetTableModel slowRstm = parser.buildSlowQueriesRstm(50);
		assertEquals(2, slowRstm.getRowCount(), "Should have 2 slow query groups (statement group + auto_explain group)");
		assertEquals(2, slowRstm.getValueAsInteger(0, "exec_count"), "First group should have 2 executions");
		assertEquals(6.0, slowRstm.getValueAsDouble(0, "duration_sec__sum"), "2500.4 + 3499.6 ms = 6.00 s");
		assertEquals(1, slowRstm.getValueAsInteger(1, "has_plan_count"), "Second group should have 1 auto_explain plan");

		System.out.println("  [OK] testBgParserAllCategories passed");
	}

	public static void testPostgresErrorLogDsrEndToEndWithH2()
	throws Exception
	{
		try (DbxConnection conn = DbxConnection.createDbxConnection(DriverManager.getConnection("jdbc:h2:mem:pgerrlog_test;DB_CLOSE_DELAY=-1")))
		{
			try (Statement st = conn.createStatement())
			{
				st.execute("CREATE TABLE \"CmErrorLog_abs\" ("
						+ "\"SessionSampleTime\" TIMESTAMP, "
						+ "\"log_time\" TIMESTAMP, "
						+ "\"user_name\" VARCHAR(100), "
						+ "\"database_name\" VARCHAR(100), "
						+ "\"process_id\" INT, "
						+ "\"connection_from\" VARCHAR(100), "
						+ "\"error_severity\" VARCHAR(30), "
						+ "\"sql_state_code\" VARCHAR(10), "
						+ "\"sql_state_code_desc\" VARCHAR(200), "
						+ "\"message\" VARCHAR(4000), "
						+ "\"detail\" VARCHAR(2000), "
						+ "\"hint\" VARCHAR(1000), "
						+ "\"query\" VARCHAR(2000), "
						+ "\"application_name\" VARCHAR(100)"
						+ ")");

				st.execute("CREATE TABLE \"MonSessionDbmsConfig\" ("
						+ "\"SessionStartTime\" TIMESTAMP, "
						+ "\"ParameterName\" VARCHAR(100), "
						+ "\"CurrentValue\" VARCHAR(200)"
						+ ")");

				st.execute("INSERT INTO \"MonSessionDbmsConfig\" VALUES "
						+ "('2026-11-18 00:00:00', 'log_checkpoints', 'on'), "
						+ "('2026-11-18 00:00:00', 'log_lock_waits', 'off'), "
						+ "('2026-11-18 00:00:00', 'log_temp_files', '-1'), "
						+ "('2026-11-18 00:00:00', 'log_autovacuum_min_duration', '0')");
			}

			String ins = "INSERT INTO \"CmErrorLog_abs\" VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			try (PreparedStatement ps = conn.prepareStatement(ins))
			{
				Timestamp ts = Timestamp.valueOf("2026-11-18 12:00:00");
				// 1 ERROR row
				ps.setTimestamp(1, ts);
				ps.setTimestamp(2, ts);
				ps.setString(3, "app_user");
				ps.setString(4, "prod");
				ps.setInt(5, 1001);
				ps.setString(6, "10.0.0.5:54321");
				ps.setString(7, "ERROR");
				ps.setString(8, "40P01");
				ps.setString(9, "deadlock_detected");
				ps.setString(10, "deadlock detected");
				ps.setString(11, "Process 1001 waits for ShareLock on transaction 555; blocked by process 1002.");
				ps.setString(12, "See server log for query details.");
				ps.setString(13, "UPDATE accounts SET balance = balance - 10 WHERE id = 1");
				ps.setString(14, "payment-svc");
				ps.executeUpdate();

				// 1 Checkpoint Complete row with slow fsync
				ps.setString(7, "LOG");
				ps.setString(8, "00000");
				ps.setString(9, "successful_completion");
				ps.setString(10, "checkpoint complete: wrote 500 buffers (3.1%); 0 WAL file(s) added, 0 removed, 1 recycled; "
						+ "write=5.100 s, sync=1.800 s, total=6.950 s; sync files=10, longest=1.500 s, average=0.180 s; distance=16384 kB, estimate=16384 kB");
				ps.setString(11, null);
				ps.setString(12, null);
				ps.setString(13, null);
				ps.executeUpdate();

				// 1 Autovacuum row
				ps.setString(10, "automatic vacuum to prevent wraparound of table \"prod.public.ledger\": index scans: 1\n"
						+ "pages: 0 removed, 2000 remain\n"
						+ "tuples: 5000 removed, 100000 remain, 150000 are dead but not yet removable\n"
						+ "buffer usage: 1000 hits, 200 misses, 50 dirtied\n"
						+ "WAL usage: 300 records, 20 full page images, 163840 bytes\n"
						+ "system usage: CPU: user: 0.20 s, system: 0.05 s, elapsed: 2.10 s");
				ps.executeUpdate();
			}

			DailySummaryReportDefault report = new DailySummaryReportDefault();
			PostgresErrorLog entry = new PostgresErrorLog(report);
			entry.create(conn, "pg-test-srv", new Configuration(), new Configuration());

			assertTrue(entry.getCountRstm() != null && entry.getCountRstm().getRowCount() == 1, "Should have 1 error group");
			assertTrue(entry.getCheckpointsRstm() != null && entry.getCheckpointsRstm().getRowCount() == 1, "Should have 1 checkpoint group");
			assertTrue(entry.getAutovacuumRstm() != null && entry.getAutovacuumRstm().getRowCount() == 1, "Should have 1 autovacuum group");
			assertEquals(3, entry.getVerdicts().size(), "Should generate 3 verdicts (slow fsync, anti-wraparound vacuum, and unremovable dead tuples)");

			StringWriter sw = new StringWriter();
			entry.writeMessageText(sw, MessageType.FULL_MESSAGE);
			String html = sw.toString();

			assertTrue(html.contains("Logged Checkpoints"), "HTML should contain Checkpoints section");
			assertTrue(html.contains("Logged Autovacuum"), "HTML should contain Autovacuum section");
			assertTrue(html.contains("log_lock_waits = on"), "HTML should suggest enabling log_lock_waits");
			assertTrue(!html.contains("<user>") && !html.contains("<project>"), "HTML must not contain unescaped <user> or <project> tags");
		}
		System.out.println("  [OK] testPostgresErrorLogDsrEndToEndWithH2 passed");
	}

	private static java.util.List<Object> createLogRow(Timestamp ts, String severity, String sqlState, String message)
	{
		CloudLogEntry entry = new CloudLogEntry();
		entry.setLogTime(ts);
		entry.setErrorSeverity(severity);
		entry.setSqlStateCode(sqlState);
		entry.setMessage(message);
		entry.setLogfile("postgres.log");
		return entry.toCmErrorLogRow();
	}

	public static void testCmErrorLogLiveTrendGraphs()
	throws Exception
	{
		if (!CounterController.hasInstance())
			CounterController.setInstance(new CounterControllerNoOp(false));

		CmErrorLog cm = new CmErrorLog(CounterController.getInstance(), null);

		TrendGraphDataPoint sevGraph       = cm.getTrendGraphData(CmErrorLog.GRAPH_NAME_SEVERITY_COUNT);
		TrendGraphDataPoint sevPerSecGraph = cm.getTrendGraphData(CmErrorLog.GRAPH_NAME_SEVERITY_PER_SEC);
		TrendGraphDataPoint opsGraph       = cm.getTrendGraphData(CmErrorLog.GRAPH_NAME_OPERATIONAL_EVENTS);
		TrendGraphDataPoint opsPerSecGraph = cm.getTrendGraphData(CmErrorLog.GRAPH_NAME_OPERATIONAL_PER_SEC);
		assertTrue(sevGraph       != null, "SeverityCount trend graph should be registered on CmErrorLog");
		assertTrue(sevPerSecGraph != null, "SeverityPerSec trend graph should be registered on CmErrorLog");
		assertTrue(opsGraph       != null, "OperationalEvents trend graph should be registered on CmErrorLog");
		assertTrue(opsPerSecGraph != null, "OperationalEventsPerSec trend graph should be registered on CmErrorLog");

		try (DbxConnection conn = DbxConnection.createDbxConnection(DriverManager.getConnection("jdbc:h2:mem:pgerrlog_graph_test;DB_CLOSE_DELAY=-1")))
		{
			CounterController.getInstance().setMonConnection(conn);
			String schemaSql = "SELECT "
					+ "CAST(NULL AS TIMESTAMP) AS \"log_time\", "
					+ "CAST(NULL AS VARCHAR(128)) AS \"user_name\", "
					+ "CAST(NULL AS VARCHAR(128)) AS \"database_name\", "
					+ "CAST(NULL AS INT) AS \"process_id\", "
					+ "CAST(NULL AS VARCHAR(128)) AS \"connection_from\", "
					+ "CAST(NULL AS VARCHAR(128)) AS \"session_id\", "
					+ "CAST(NULL AS BIGINT) AS \"session_line_num\", "
					+ "CAST(NULL AS VARCHAR(128)) AS \"command_tag\", "
					+ "CAST(NULL AS VARCHAR(128)) AS \"session_start_time\", "
					+ "CAST(NULL AS VARCHAR(32)) AS \"virtual_transaction_id\", "
					+ "CAST(NULL AS VARCHAR(32)) AS \"transaction_id\", "
					+ "CAST(NULL AS VARCHAR(15)) AS \"error_severity\", "
					+ "CAST(NULL AS VARCHAR(15)) AS \"sql_state_code\", "
					+ "CAST(NULL AS VARCHAR(60)) AS \"sql_state_code_desc\", "
					+ "CAST(NULL AS VARCHAR(4000)) AS \"message\", "
					+ "CAST(NULL AS VARCHAR(512)) AS \"detail\", "
					+ "CAST(NULL AS VARCHAR(512)) AS \"hint\", "
					+ "CAST(NULL AS VARCHAR(4000)) AS \"internal_query\", "
					+ "CAST(NULL AS INT) AS \"internal_query_pos\", "
					+ "CAST(NULL AS VARCHAR(512)) AS \"context\", "
					+ "CAST(NULL AS VARCHAR(4000)) AS \"query\", "
					+ "CAST(NULL AS BIGINT) AS \"query_pos\", "
					+ "CAST(NULL AS VARCHAR(128)) AS \"location\", "
					+ "CAST(NULL AS VARCHAR(128)) AS \"application_name\", "
					+ "CAST(NULL AS VARCHAR(128)) AS \"backend_type\", "
					+ "CAST(NULL AS INT) AS \"leader_pid\", "
					+ "CAST(NULL AS BIGINT) AS \"query_id\", "
					+ "CAST(NULL AS VARCHAR(128)) AS \"pg_current_logfile\" "
					+ "WHERE 1=2";

			// 1. First sample: should NOT populate trend graphs (avoids startup log backlog spike)
			CounterSample firstSample = new CounterSample("test1", true, new String[0], null);
			firstSample.getSample(cm, conn, schemaSql, null);
			firstSample.addRow(cm, createLogRow(
					Timestamp.valueOf("2026-11-18 12:00:00"), "ERROR", "40P01", "deadlock detected"));
			cm.localCalculation(firstSample);
			cm.updateGraphData();
			assertTrue(!sevGraph.hasData(), "First time sample should not emit fake backlog spike into SeverityCount graph");

			// 2. Second sample (live monitoring, 10s interval): 1 ERROR, 1 WARNING, 5 LOG (checkpoint, autovacuum, autoanalyze, temp file, slow query)
			cm.setFirstTimeSample(false);
			CounterSample secondSample = new CounterSample("test2", true, new String[0], firstSample);
			secondSample.getSample(cm, conn, schemaSql, null);
			secondSample.setSampleInterval(10_000L); // 10 seconds -> 1 event = 0.10/sec, 5 events = 0.50/sec
			Timestamp ts2 = Timestamp.valueOf("2026-11-18 12:01:00");
			secondSample.addRow(cm, createLogRow(ts2, "ERROR", "23505", "duplicate key value"));
			secondSample.addRow(cm, createLogRow(ts2, "WARNING", "01000", "nonstandard use of \\\\ in a string literal"));
			secondSample.addRow(cm, createLogRow(ts2, "LOG", "00000",
					"checkpoint complete: wrote 100 buffers (1.0%); 0 WAL file(s) added, 0 removed, 1 recycled; write=1.000 s, sync=0.010 s, total=1.020 s; sync files=5, longest=0.005 s, average=0.002 s; distance=1024 kB, estimate=1024 kB"));
			secondSample.addRow(cm, createLogRow(ts2, "LOG", "00000",
					"automatic vacuum of table \"prod.public.t1\": index scans: 0"));
			secondSample.addRow(cm, createLogRow(ts2, "LOG", "00000",
					"automatic analyze of table \"prod.public.t1\""));
			secondSample.addRow(cm, createLogRow(ts2, "LOG", "00000",
					"temporary file: path \"base/pgsql_tmp/1\", size 10485760"));
			secondSample.addRow(cm, createLogRow(ts2, "LOG", "00000",
					"duration: 1500.000 ms  statement: SELECT 1"));

			cm.localCalculation(secondSample);
			cm.updateGraphData();

			Double[] sevData = sevGraph.getData();
			// [PANIC, FATAL, ERROR, WARNING, LOG]
			assertEquals(Arrays.asList(0.0, 0.0, 1.0, 1.0, 5.0), Arrays.asList(sevData), "SeverityCount graph data on sample 2");
			assertEquals(Arrays.asList(0.0, 0.0, 0.1, 0.1, 0.5), Arrays.asList(sevPerSecGraph.getData()), "SeverityPerSec graph data on sample 2 (10s interval)");

			Double[] opsData = opsGraph.getData();
			// [Checkpoints, Autovacuums, Autoanalyzes, TempFiles, LockWaits, SlowQueries]
			assertEquals(Arrays.asList(1.0, 1.0, 1.0, 1.0, 0.0, 1.0), Arrays.asList(opsData), "OperationalEvents graph data on sample 2");
			assertEquals(Arrays.asList(0.1, 0.1, 0.1, 0.1, 0.0, 0.1), Arrays.asList(opsPerSecGraph.getData()), "OperationalEventsPerSec graph data on sample 2 (10s interval)");

			// 3. Third sample (quiet sample with 0 rows): must reset graph points to 0.0 instead of keeping sample 2's values!
			CounterSample thirdSample = new CounterSample("test3", true, new String[0], secondSample);
			thirdSample.getSample(cm, conn, schemaSql, null);
			thirdSample.setSampleInterval(10_000L);
			cm.localCalculation(thirdSample);
			cm.updateGraphData();

			assertEquals(Arrays.asList(0.0, 0.0, 0.0, 0.0, 0.0), Arrays.asList(sevGraph.getData()), "Quiet sample 3 must zero SeverityCount");
			assertEquals(Arrays.asList(0.0, 0.0, 0.0, 0.0, 0.0), Arrays.asList(sevPerSecGraph.getData()), "Quiet sample 3 must zero SeverityPerSec");
			assertEquals(Arrays.asList(0.0, 0.0, 0.0, 0.0, 0.0, 0.0), Arrays.asList(opsGraph.getData()), "Quiet sample 3 must zero OperationalEvents");
			assertEquals(Arrays.asList(0.0, 0.0, 0.0, 0.0, 0.0, 0.0), Arrays.asList(opsPerSecGraph.getData()), "Quiet sample 3 must zero OperationalEventsPerSec");
		}
		System.out.println("  [OK] testCmErrorLogLiveTrendGraphs passed");
	}

	public static void main(String[] args)
	throws Exception
	{
		System.out.println("Running PostgresErrorLogAnalyticsTest...");
		testBgParserAllCategories();
		testPostgresErrorLogDsrEndToEndWithH2();
		testCmErrorLogLiveTrendGraphs();
		System.out.println("ALL POSTGRES ERRORLOG ANALYTICS TESTS PASSED!");
	}
}
