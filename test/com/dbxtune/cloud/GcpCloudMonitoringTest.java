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
package com.dbxtune.cloud;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.dbxtune.hostmon.HostMonitorConnectionCloud;
import com.dbxtune.hostmon.MonitorDiskSpace;
import com.dbxtune.hostmon.MonitorDiskSpaceCloud;
import com.dbxtune.hostmon.MonitorIo;
import com.dbxtune.hostmon.MonitorIoCloud;
import com.dbxtune.hostmon.MonitorMeminfo;
import com.dbxtune.hostmon.MonitorMeminfoCloud;
import com.dbxtune.hostmon.MonitorMpstat;
import com.dbxtune.hostmon.MonitorMpstatCloud;
import com.dbxtune.hostmon.MonitorNwInfo;
import com.dbxtune.hostmon.MonitorNwInfoCloud;
import com.dbxtune.hostmon.MonitorUpTime;
import com.dbxtune.hostmon.MonitorUpTimeCloud;
import com.dbxtune.hostmon.OsTable;
import com.dbxtune.utils.Configuration;

public class GcpCloudMonitoringTest
{
	private static void assertEquals(Object expected, Object actual)
	{
		if (!Objects.equals(expected, actual))
			throw new AssertionError("Expected <" + expected + "> but was <" + actual + ">");
	}

	private static void assertEquals(double expected, double actual, double delta)
	{
		if (Math.abs(expected - actual) > delta)
			throw new AssertionError("Expected <" + expected + "> (+/- " + delta + ") but was <" + actual + ">");
	}

	private static void assertTrue(boolean condition)
	{
		if (!condition)
			throw new AssertionError("Expected true but was false");
	}

	private static void assertFalse(boolean condition)
	{
		if (condition)
			throw new AssertionError("Expected false but was true");
	}

	private static void assertNotNull(Object obj)
	{
		if (obj == null)
			throw new AssertionError("Expected non-null object");
	}

	private static void assertNull(Object obj)
	{
		if (obj != null)
			throw new AssertionError("Expected null but was <" + obj + ">");
	}

	public void testCloudConfigShorthandParsing()
	{
		// 1. Explicit URI scheme: gcp://project:region:instance
		CloudConfig cfg1 = CloudConfig.fromShorthand("gcp://my-project:europe-north1:pg-prod-01", null, "/path/to/sa.json");
		assertNotNull(cfg1);
		assertEquals(CloudProvider.GCP, cfg1.getProvider());
		assertEquals("my-project", cfg1.getProjectId());
		assertEquals("europe-north1", cfg1.getRegion());
		assertEquals("pg-prod-01", cfg1.getInstanceId());
		assertEquals("my-project:pg-prod-01", cfg1.getGcpDatabaseId());
		assertEquals("/path/to/sa.json", cfg1.getCredentialsFile());
		assertTrue(cfg1.isValid());

		// 2. Three-part shorthand: project:region:instance (no username needed)
		CloudConfig cfg2 = CloudConfig.fromShorthand("my-project:us-central1:pg-instance", null, null);
		assertNotNull(cfg2);
		assertEquals(CloudProvider.GCP, cfg2.getProvider());
		assertEquals("my-project", cfg2.getProjectId());
		assertEquals("us-central1", cfg2.getRegion());
		assertEquals("pg-instance", cfg2.getInstanceId());
		assertTrue(cfg2.isValid());

		// 3. Two-part shorthand with sshUsername = "gcp": project:instance
		CloudConfig cfg3 = CloudConfig.fromShorthand("my-project:pg-instance", "gcp", null);
		assertNotNull(cfg3);
		assertEquals(CloudProvider.GCP, cfg3.getProvider());
		assertEquals("my-project", cfg3.getProjectId());
		assertEquals("", cfg3.getRegion());
		assertEquals("pg-instance", cfg3.getInstanceId());
		assertEquals("my-project:pg-instance", cfg3.getGcpDatabaseId());
		assertTrue(cfg3.isValid());

		// 4. Non-cloud SSH hostnames should return null
		assertNull(CloudConfig.fromShorthand("192.168.1.50", "postgres", null));
		assertNull(CloudConfig.fromShorthand("dbserver.example.com:2222", "root", null));
		assertNull(CloudConfig.fromShorthand("localhost", "gorans", null));
	}

	public void testCloudConfigFromConfiguration()
	{
		Configuration conf = new Configuration();
		conf.setProperty(CloudConfig.PROPKEY_provider, "gcp");
		conf.setProperty(CloudConfig.PROPKEY_project, "conf-proj");
		conf.setProperty(CloudConfig.PROPKEY_region, "europe-west1");
		conf.setProperty(CloudConfig.PROPKEY_instance, "conf-inst");
		conf.setProperty(CloudConfig.PROPKEY_credentialsFile, "/etc/gcp/key.json");
		conf.setProperty(CloudConfig.PROPKEY_cacheTtlSec, 45);

		CloudConfig cfg = CloudConfig.fromConfiguration(conf, null, null, null);
		assertNotNull(cfg);
		assertEquals(CloudProvider.GCP, cfg.getProvider());
		assertEquals("conf-proj", cfg.getProjectId());
		assertEquals("europe-west1", cfg.getRegion());
		assertEquals("conf-inst", cfg.getInstanceId());
		assertEquals("/etc/gcp/key.json", cfg.getCredentialsFile());
		assertEquals(45000L, cfg.getCacheTtlMs());
		assertTrue(cfg.isValid());
	}

	public void testGcpLoggingClientJsonParsing() throws Exception
	{
		String jsonResponse = "{\n"
			+ "  \"entries\": [\n"
			+ "    {\n"
			+ "      \"insertId\": \"e1\",\n"
			+ "      \"timestamp\": \"2026-11-18T14:20:00.123456Z\",\n"
			+ "      \"severity\": \"ERROR\",\n"
			+ "      \"textPayload\": \"2026-11-18 14:20:00.123 UTC [4321]: [1-1] db=mydb,user=pgadmin,app=psql,client=10.1.2.3 ERROR:  42P01: relation \\\"missing_table\\\" does not exist at character 15\\nSTATEMENT:  SELECT * FROM missing_table;\"\n"
			+ "    },\n"
			+ "    {\n"
			+ "      \"insertId\": \"e2\",\n"
			+ "      \"timestamp\": \"2026-11-18T14:20:05.500000Z\",\n"
			+ "      \"severity\": \"ERROR\",\n"
			+ "      \"textPayload\": \"2026-11-18 14:20:05.500 UTC [4322]: [1-1] db=mydb,user=appuser ERROR:  duplicate key value violates unique constraint \\\"users_pkey\\\"\"\n"
			+ "    },\n"
			+ "    {\n"
			+ "      \"insertId\": \"e3\",\n"
			+ "      \"timestamp\": \"2026-11-18T14:20:05.500100Z\",\n"
			+ "      \"severity\": \"INFO\",\n"
			+ "      \"textPayload\": \"2026-11-18 14:20:05.500 UTC [4322]: [2-1] db=mydb,user=appuser DETAIL:  Key (id)=(99) already exists.\"\n"
			+ "    },\n"
			+ "    {\n"
			+ "      \"insertId\": \"e4\",\n"
			+ "      \"timestamp\": \"2026-11-18T14:20:10.000000Z\",\n"
			+ "      \"severity\": \"LOG\",\n"
			+ "      \"textPayload\": \"2026-11-18 14:20:10.000 UTC [5555]: [1-1] db=mydb,user=appuser LOG:  duration: 254.120 ms  plan:\\nQuery Text: SELECT * FROM orders WHERE customer_id = $1\\nIndex Scan using idx_orders_cust on orders  (cost=0.42..8.44 rows=1 width=64)\"\n"
			+ "    }\n"
			+ "  ]\n"
			+ "}";

		CloudConfig cfg = new CloudConfig(CloudProvider.GCP, "test-proj", "europe-north1", "test-inst", "");
		GcpLoggingClient client = new GcpLoggingClient(cfg, null);

		List<CloudLogEntry> entries = client.parseEntriesListResponse(jsonResponse);
		// e2 and e3 should be merged because e3 is a DETAIL continuation for the same PID 4322
		assertEquals(3, entries.size());

		// Check Entry 1: explicit SQLSTATE 42P01 + inline STATEMENT
		CloudLogEntry row1 = entries.get(0);
		assertEquals("pgadmin", row1.getUserName());
		assertEquals("mydb", row1.getDatabaseName());
		assertEquals(Integer.valueOf(4321), row1.getProcessId());
		assertEquals("10.1.2.3", row1.getConnectionFrom());
		assertEquals("psql", row1.getApplicationName());
		assertEquals("ERROR", row1.getErrorSeverity());
		assertEquals("42P01", row1.getSqlStateCode());
		assertTrue(row1.getMessage().contains("relation \"missing_table\" does not exist"));
		assertEquals("SELECT * FROM missing_table;", row1.getQuery());

		// Check Entry 2: inferred SQLSTATE 23505 + merged DETAIL continuation
		CloudLogEntry row2 = entries.get(1);
		assertEquals("appuser", row2.getUserName());
		assertEquals("mydb", row2.getDatabaseName());
		assertEquals(Integer.valueOf(4322), row2.getProcessId());
		assertEquals("ERROR", row2.getErrorSeverity());
		assertEquals("23505", row2.getSqlStateCode());
		assertTrue(row2.getMessage().contains("duplicate key value violates unique constraint"));
		assertEquals("Key (id)=(99) already exists.", row2.getDetail());

		// Check Entry 3: auto_explain plan entry
		CloudLogEntry row3 = entries.get(2);
		assertEquals(Integer.valueOf(5555), row3.getProcessId());
		assertEquals("LOG", row3.getErrorSeverity());
		assertEquals("00000", row3.getSqlStateCode());
		assertTrue(row3.getMessage().startsWith("duration: 254.120 ms  plan:"));
		assertTrue(row3.getMessage().contains("Query Text: SELECT * FROM orders"));
	}

	public void testCloudHostMonitorsPopulateOsTables() throws Exception
	{
		CloudConfig cfg = new CloudConfig(CloudProvider.GCP, "test-proj", "europe-north1", "test-inst", "");

		final CloudMetricsSnapshot snapshot = new CloudMetricsSnapshot();
		snapshot.setMetricTimestamp(Timestamp.from(Instant.now()));
		snapshot.setInstanceState("RUNNABLE");
		snapshot.setUptimeSeconds(86400L * 5 + 3660L); // 5 days, 1 hour, 1 minute
		snapshot.setCpuReservedCores(4);
		snapshot.setCpuUtilizationPct(35.0);

		snapshot.setMemoryQuotaBytes(16L * 1024 * 1024 * 1024); // 16 GB
		snapshot.setMemoryUsageBytes(10L * 1024 * 1024 * 1024); // 10 GB
		snapshot.setMemoryCacheBytes(2L * 1024 * 1024 * 1024);  // 2 GB
		snapshot.setSwapUsedBytes(0L);

		snapshot.setDiskQuotaBytes(100L * 1024 * 1024 * 1024); // 100 GB
		snapshot.setDiskBytesUsed(40L * 1024 * 1024 * 1024);   // 40 GB
		snapshot.setDiskReadOpsPerSec(120.0);
		snapshot.setDiskWriteOpsPerSec(80.0);
		snapshot.setDiskReadBytesPerSec(120.0 * 16384.0);
		snapshot.setDiskWriteBytesPerSec(80.0 * 16384.0);
		snapshot.setDiskReadOpsQuota(3000L);
		snapshot.setDiskWriteOpsQuota(3000L);

		snapshot.setNetRxBytesTotal(1024L * 500); // 500 KB
		snapshot.setNetTxBytesTotal(1024L * 300); // 300 KB
		snapshot.setActiveConnections(42);
		snapshot.computeDerivedFields();

		HostMonitorConnectionCloud mockConn = new HostMonitorConnectionCloud(cfg)
		{
			@Override
			public void connect()
			{
				_connected = true;
			}

			@Override
			protected CloudMetricsSnapshot fetchMetricsFromProvider()
			{
				return snapshot;
			}

			@Override
			public List<CloudLogEntry> fetchNewLogEntries()
			{
				return Collections.emptyList();
			}

			@Override
			public String testCloudConnection()
			{
				return "OK";
			}
		};
		mockConn.connect();
		assertTrue(mockConn.isConnected());
		assertEquals("Cloud-GCP", mockConn.getOsName());

		// 1. MonitorMpstatCloud
		MonitorMpstatCloud mpstat = new MonitorMpstatCloud();
		mpstat.setConnectedToVendor(MonitorMpstat.OsVendor.Cloud);
		mpstat.setConnection(mockConn);
		mpstat.setMetaData(mpstat.createMetaData());
		assertFalse(mpstat.isOsCommandStreaming());
		OsTable mpTable = mpstat.executeAndParse();
		assertNotNull(mpTable);
		assertEquals(1, mpTable.getRowCount());
		assertEquals("all", mpTable.getValueAt(0, 2));
		assertEquals(35.0, ((Number) mpTable.getValueAt(0, 4)).doubleValue(), 0.01);
		assertEquals(65.0, ((Number) mpTable.getValueAt(0, 12)).doubleValue(), 0.01);

		// 2. MonitorIoCloud
		MonitorIoCloud iostat = new MonitorIoCloud();
		iostat.setConnectedToVendor(MonitorIo.OsVendor.Cloud);
		iostat.setConnection(mockConn);
		iostat.setMetaData(iostat.createMetaData());
		assertFalse(iostat.isOsCommandStreaming());
		OsTable ioTable = iostat.executeAndParse();
		assertNotNull(ioTable);
		assertEquals(1, ioTable.getRowCount());
		assertEquals("cloudsql-disk", ioTable.getValueAt(0, 0));
		assertEquals(120.0, ((Number) ioTable.getValueAt(0, 4)).doubleValue(), 0.01); // readsPerSec
		assertEquals(80.0, ((Number) ioTable.getValueAt(0, 5)).doubleValue(), 0.01);  // writesPerSec

		// 3. MonitorMeminfoCloud
		MonitorMeminfoCloud meminfo = new MonitorMeminfoCloud();
		meminfo.setConnectedToVendor(MonitorMeminfo.OsVendor.Cloud);
		meminfo.setConnection(mockConn);
		meminfo.setMetaData(meminfo.createMetaData());
		OsTable memTable = meminfo.executeAndParse();
		assertNotNull(memTable);
		assertTrue(memTable.getRowCount() >= 5);
		assertEquals("MemTotal", memTable.getValueAt(0, 0));

		// 4. MonitorDiskSpaceCloud
		MonitorDiskSpaceCloud diskSpace = new MonitorDiskSpaceCloud();
		diskSpace.setConnectedToVendor(MonitorDiskSpace.OsVendor.Cloud);
		diskSpace.setConnection(mockConn);
		diskSpace.setMetaData(diskSpace.createMetaData());
		OsTable dfTable = diskSpace.executeAndParse();
		assertNotNull(dfTable);
		assertEquals(1, dfTable.getRowCount());
		assertEquals("cloudsql-disk", dfTable.getValueAt(0, 0));
		assertEquals("/var/lib/postgresql/data", dfTable.getValueAt(0, 8));

		// 5. MonitorNwInfoCloud
		MonitorNwInfoCloud nwInfo = new MonitorNwInfoCloud();
		nwInfo.setConnectedToVendor(MonitorNwInfo.OsVendor.Cloud);
		nwInfo.setConnection(mockConn);
		nwInfo.setMetaData(nwInfo.createMetaData());
		OsTable nwTable = nwInfo.executeAndParse();
		assertNotNull(nwTable);
		assertEquals(1, nwTable.getRowCount());
		assertEquals("cloudsql-nic0", nwTable.getValueAt(0, 0));

		// 6. MonitorUpTimeCloud
		MonitorUpTimeCloud upTime = new MonitorUpTimeCloud();
		upTime.setConnectedToVendor(MonitorUpTime.OsVendor.Cloud);
		upTime.setConnection(mockConn);
		upTime.setMetaData(upTime.createMetaData());
		OsTable upTable = upTime.executeAndParse();
		assertNotNull(upTable);
		assertEquals(1, upTable.getRowCount());
		assertEquals(5, ((Number) upTable.getValueAt(0, 1)).intValue()); // 5 days

		// Verify offline metadata creation works for all 6 monitors
		assertNotNull(MonitorMpstat.createOfflineMetaData());
		assertNotNull(MonitorIo.createOfflineMetaData());
		assertNotNull(MonitorMeminfo.createOfflineMetaData());
		assertNotNull(MonitorDiskSpace.createOfflineMetaData());
		assertNotNull(MonitorNwInfo.createOfflineMetaData());
		assertNotNull(MonitorUpTime.createOfflineMetaData());
	}

	public void testGcpIamPostgresAuthHelpers()
	{
		assertTrue(GcpCredentialsHelper.isGcpIamUsername("dbxtune-monitor@my-project.iam"));
		assertTrue(GcpCredentialsHelper.isGcpIamUsername("dbxtune-monitor@my-project.iam.gserviceaccount.com"));
		assertFalse(GcpCredentialsHelper.isGcpIamUsername("dbxtune"));
		assertFalse(GcpCredentialsHelper.isGcpIamUsername("postgres"));

		assertEquals("dbxtune-monitor@my-project.iam",
				GcpCredentialsHelper.normalizeGcpIamPostgresUsername("dbxtune-monitor@my-project.iam.gserviceaccount.com"));
		assertEquals("dbxtune-monitor@my-project.iam",
				GcpCredentialsHelper.normalizeGcpIamPostgresUsername("dbxtune-monitor@my-project.iam"));

		// IAM user + blank password on postgresql URL -> true
		assertTrue(GcpCredentialsHelper.isGcpIamPostgresAuth("jdbc:postgresql://10.0.0.5:5432/postgres", "dbxtune-monitor@my-project.iam", ""));
		assertTrue(GcpCredentialsHelper.isGcpIamPostgresAuth("jdbc:postgresql://10.0.0.5:5432/postgres", "dbxtune-monitor@my-project.iam.gserviceaccount.com", null));
		// Explicit gcp-iam password trigger -> true
		assertTrue(GcpCredentialsHelper.isGcpIamPostgresAuth("jdbc:postgresql://10.0.0.5:5432/postgres", "custom_iam_user", "gcp-iam"));
		assertTrue(GcpCredentialsHelper.isGcpIamPostgresAuth("jdbc:postgresql://10.0.0.5:5432/postgres", "custom_iam_user", "gcp-iam:/etc/dbxtune/sa.json"));
		// Standard Postgres user with a password -> false
		assertFalse(GcpCredentialsHelper.isGcpIamPostgresAuth("jdbc:postgresql://10.0.0.5:5432/postgres", "dbxtune", "secretPass123"));
		// Non-Postgres JDBC URL -> false
		assertFalse(GcpCredentialsHelper.isGcpIamPostgresAuth("jdbc:sqlserver://10.0.0.5:1433", "dbxtune-monitor@my-project.iam", ""));

		assertEquals("/etc/dbxtune/sa.json", GcpCredentialsHelper.resolveCredentialsFile("gcp-iam:/etc/dbxtune/sa.json", null));
		// The Key File field is only used when it is a GCP credentials JSON file (a missing file or an SSH key is not): see testGcpCredentialsFileCheck()
		assertEquals("", GcpCredentialsHelper.resolveCredentialsFile("", "/etc/dbxtune/does-not-exist.json"));

		// Verify '-u gcp' auto-resolves instanceId from JDBC hostname (or -A alias when JDBC host is an IP)
		// and projectId from the IAM DBMS username!
		Configuration conf1 = new Configuration();
		conf1.setProperty("conn.dbmsUsername", "dbxtune-monitor@my-prod-project.iam");
		CloudConfig autoCfg1 = CloudConfig.fromConfiguration(conf1, "pg-orders-01.corp.internal", "gcp", null);
		assertNotNull(autoCfg1);
		assertEquals(CloudProvider.GCP, autoCfg1.getProvider());
		assertEquals("my-prod-project", autoCfg1.getProjectId());
		assertEquals("pg-orders-01", autoCfg1.getInstanceId());

		// When JDBC host is a raw IP (10.20.30.40) and -A (conn.dbmsServerAlias) is pg-orders-01:
		Configuration conf2 = new Configuration();
		conf2.setProperty("conn.dbmsUsername", "dbxtune-monitor@my-prod-project.iam");
		conf2.setProperty("conn.dbmsServerAlias", "pg-orders-01");
		CloudConfig autoCfg2 = CloudConfig.fromConfiguration(conf2, "10.20.30.40", "gcp", null);
		assertNotNull(autoCfg2);
		assertEquals("my-prod-project", autoCfg2.getProjectId());
		assertEquals("pg-orders-01", autoCfg2.getInstanceId());

		// A DNS name that is NOT the instance (Private Service Connect): the guess from the hostname must not override
		String pscHost = "team-p-shared-pg16-db-psc.sandbox.internal-maxm.se";

		// ... the explicit 'conn.hostmon.cloud.instance' wins
		Configuration conf3 = new Configuration();
		conf3.setProperty("conn.dbmsUsername", "dbxtune-monitor@mgmt-vms-02b8b9eb.iam");
		conf3.setProperty(CloudConfig.PROPKEY_project,  "sandbox-team-p-1920f585");
		conf3.setProperty(CloudConfig.PROPKEY_instance, "sandbox-team-p-shared-pg16-c7d48d");
		CloudConfig autoCfg3 = CloudConfig.fromConfiguration(conf3, pscHost, "gcp", null);
		assertEquals("sandbox-team-p-1920f585",           autoCfg3.getProjectId());
		assertEquals("sandbox-team-p-shared-pg16-c7d48d", autoCfg3.getInstanceId());

		// ... also when the hostname has a port (host:5432)
		CloudConfig autoCfg3b = CloudConfig.fromConfiguration(conf3, pscHost + ":5432", "gcp", null);
		assertEquals("sandbox-team-p-shared-pg16-c7d48d", autoCfg3b.getInstanceId());

		// ... '-A' (conn.dbmsServerAlias) is a display name: it does NOT override the name from the hostname (only a fallback when there is none, see conf2)
		Configuration conf4 = new Configuration();
		conf4.setProperty("conn.dbmsUsername", "dbxtune-monitor@mgmt-vms-02b8b9eb.iam");
		conf4.setProperty("conn.dbmsServerAlias", "GCP_test_srv_1");
		CloudConfig autoCfg4 = CloudConfig.fromConfiguration(conf4, pscHost, "gcp", null);
		assertEquals("team-p-shared-pg16-db-psc", autoCfg4.getInstanceId());

		// ... and with the explicit properties, '-A' is just the name
		conf3.setProperty("conn.dbmsServerAlias", "GCP_test_srv_1");
		CloudConfig autoCfg4b = CloudConfig.fromConfiguration(conf3, pscHost, "gcp", null);
		assertEquals("sandbox-team-p-1920f585",           autoCfg4b.getProjectId());
		assertEquals("sandbox-team-p-shared-pg16-c7d48d", autoCfg4b.getInstanceId());
		conf3.remove("conn.dbmsServerAlias");

		// ... but an instance written in the host field ('-s project:instance' or 'gcp://...') still wins over the property
		CloudConfig autoCfg5 = CloudConfig.fromConfiguration(conf3, "other-proj:other-inst", "gcp", null);
		assertEquals("other-proj", autoCfg5.getProjectId());
		assertEquals("other-inst", autoCfg5.getInstanceId());

		CloudConfig autoCfg5b = CloudConfig.fromConfiguration(conf3, "gcp://other-inst", null, null);
		assertEquals("other-inst", autoCfg5b.getInstanceId());
	}

	/** An entry that arrives late (older log time than one already read) must still be read; the overlap of two reads must not give duplicates */
	public void testGcpLoggingLateArrivalAndOverlap() throws Exception
	{
		CloudConfig cfg = CloudConfig.fromShorthand("gcp://my-project:europe-north1:pg-01", null, null);
		GcpLoggingClient client = new GcpLoggingClient(cfg, null);

		// Times after "now" (the client starts its watermark at now - 60s)
		Instant t0 = Instant.now().plusSeconds(10).truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
		String aLog = t0.plusSeconds(5).toString(), aRecv = t0.plusSeconds(6) .toString();
		String bLog = t0.plusSeconds(1).toString(), bRecv = t0.plusSeconds(20).toString();

		String read1 = "{ \"entries\": [ "
			+ "{ \"insertId\": \"a1\", \"timestamp\": \"" + aLog + "\", \"receiveTimestamp\": \"" + aRecv + "\", \"severity\": \"ERROR\", \"textPayload\": \"[100]: [1-1] db=d,user=u ERROR:  first\" } "
			+ "] }";
		List<CloudLogEntry> rows1 = client.parseEntriesListResponse(read1);
		assertEquals(1, rows1.size());
		assertEquals(Instant.parse(aRecv), client.getWatermark());

		// The next read overlaps (a1 again), and b1 arrived late: its log time is BEFORE a1, but it was received later
		String read2 = "{ \"entries\": [ "
			+ "{ \"insertId\": \"b1\", \"timestamp\": \"" + bLog + "\", \"receiveTimestamp\": \"" + bRecv + "\", \"severity\": \"ERROR\", \"textPayload\": \"[101]: [1-1] db=d,user=u ERROR:  late\" }, "
			+ "{ \"insertId\": \"a1\", \"timestamp\": \"" + aLog + "\", \"receiveTimestamp\": \"" + aRecv + "\", \"severity\": \"ERROR\", \"textPayload\": \"[100]: [1-1] db=d,user=u ERROR:  first\" } "
			+ "] }";
		List<CloudLogEntry> rows2 = client.parseEntriesListResponse(read2);
		assertEquals(1, rows2.size());                       // only the late one, not a1 again
		assertTrue(rows2.get(0).getMessage().contains("late"));
		assertEquals(Instant.parse(bRecv), client.getWatermark()); // on receive time
	}

	/** The Host Monitor 'Key File' field can hold an SSH private key: only a GCP credentials JSON file may be used as GCP credentials */
	public void testGcpCredentialsFileCheck() throws Exception
	{
		java.nio.file.Path json = java.nio.file.Files.createTempFile("dbxtune-gcp-", ".json");
		java.nio.file.Path pem  = java.nio.file.Files.createTempFile("dbxtune-ssh-", ".key");
		try
		{
			java.nio.file.Files.writeString(json, "{ \"type\": \"service_account\", \"project_id\": \"my-project\" }");
			java.nio.file.Files.writeString(pem , "-----BEGIN OPENSSH PRIVATE KEY-----\nAAAAB3NzaC1yc2E\n-----END OPENSSH PRIVATE KEY-----\n");

			assertTrue (GcpCredentialsHelper.isGcpCredentialsFile(json.toString()));
			assertFalse(GcpCredentialsHelper.isGcpCredentialsFile(pem .toString()));
			assertFalse(GcpCredentialsHelper.isGcpCredentialsFile(""));
			assertFalse(GcpCredentialsHelper.isGcpCredentialsFile(json.toString() + ".does-not-exist"));

			// An SSH key in the Key File field is not taken as the GCP credentials file
			assertEquals(""             , GcpCredentialsHelper.resolveCredentialsFile(null, pem .toString()));
			assertEquals(json.toString(), GcpCredentialsHelper.resolveCredentialsFile(null, json.toString()));

			// Loading an SSH key as credentials gives a clear message (not a JSON parse error)
			try
			{
				GcpCredentialsHelper.loadCredentialsFile(pem);
				throw new AssertionError("Expected an IOException for an SSH key file");
			}
			catch (java.io.IOException ex)
			{
				assertTrue(ex.getMessage().contains("is not a GCP credentials JSON file"));
			}
		}
		finally
		{
			java.nio.file.Files.deleteIfExists(json);
			java.nio.file.Files.deleteIfExists(pem);
		}
	}

	/** conn.dbms.cloud.iamAuth=true only applies to the monitored connection (and the same user), not to every connection with an empty password */
	public void testGcpIamAuthFlagScope() throws Exception
	{
		String url = "jdbc:postgresql://10.0.0.5:5432/postgres";
		Configuration conf = new Configuration();
		conf.setProperty(GcpCredentialsHelper.PROPKEY_dbmsIamAuth, "true");
		conf.setProperty(GcpCredentialsHelper.PROPKEY_dbmsCredentialsFile, "/does-not-exist/sa.json"); // no token: fails fast, no network
		Configuration.setInstance(Configuration.USER_TEMP, conf);
		try
		{
			// Not the monitored connection, and no monitored IAM user yet: the property does not apply
			assertFalse(GcpCredentialsHelper.isGcpIamPostgresAuth(url, "monuser", "", false));
			// The monitored connection: it applies
			assertTrue (GcpCredentialsHelper.isGcpIamPostgresAuth(url, "monuser", "", true));

			// The monitored connection logs in (no token here, but its user is remembered)
			java.util.Properties props = new java.util.Properties();
			props.setProperty("user", "monuser");
			props.setProperty("password", "");
			try { GcpCredentialsHelper.applyGcpIamPostgresAuth(url, props, null, true); }
			catch (java.sql.SQLException expected) { }

			// The collector's other connections with the same user: applies. Another user with an empty password (a PCS store...): does not
			assertTrue (GcpCredentialsHelper.isGcpIamPostgresAuth(url, "monuser", "", false));
			assertFalse(GcpCredentialsHelper.isGcpIamPostgresAuth(url, "pcsuser", "", false));
			// A password is never replaced
			assertFalse(GcpCredentialsHelper.isGcpIamPostgresAuth(url, "monuser", "secretPass123", true));
		}
		finally
		{
			conf.remove(GcpCredentialsHelper.PROPKEY_dbmsIamAuth);
			conf.remove(GcpCredentialsHelper.PROPKEY_dbmsCredentialsFile);
		}
	}

	public static void main(String[] args) throws Exception
	{
		GcpCloudMonitoringTest test = new GcpCloudMonitoringTest();
		test.testGcpIamAuthFlagScope();
		System.out.println("PASS: testGcpIamAuthFlagScope");
		test.testGcpLoggingLateArrivalAndOverlap();
		System.out.println("PASS: testGcpLoggingLateArrivalAndOverlap");
		test.testGcpCredentialsFileCheck();
		System.out.println("PASS: testGcpCredentialsFileCheck");
		test.testCloudConfigShorthandParsing();
		System.out.println("PASS: testCloudConfigShorthandParsing");
		test.testCloudConfigFromConfiguration();
		System.out.println("PASS: testCloudConfigFromConfiguration");
		test.testGcpLoggingClientJsonParsing();
		System.out.println("PASS: testGcpLoggingClientJsonParsing");
		test.testCloudHostMonitorsPopulateOsTables();
		System.out.println("PASS: testCloudHostMonitorsPopulateOsTables");
		test.testGcpIamPostgresAuthHelpers();
		System.out.println("PASS: testGcpIamPostgresAuthHelpers");
		System.out.println("ALL CLOUD MONITORING TESTS PASSED!");
	}
}
