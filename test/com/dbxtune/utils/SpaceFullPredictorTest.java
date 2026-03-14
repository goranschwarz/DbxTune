/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.SpaceFullPredictor.PredictionResult;
import com.dbxtune.utils.SpaceFullPredictor.SourceDataSize;

public class SpaceFullPredictorTest
{
	private Connection jdbcConn;
	private DbxConnection dbxConn;
	private SpaceFullPredictor predictor;

	@Before
	public void setUp() throws SQLException
	{
		// Create H2 in-memory database
		jdbcConn = DriverManager.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "");
		dbxConn = DbxConnection.createDbxConnection(jdbcConn);
		predictor = new SpaceFullPredictor(dbxConn);
	}

	@After
	public void tearDown() throws SQLException
	{
		if (dbxConn != null && !dbxConn.isClosed())
		{
			dbxConn.close();
		}
	}

	private void printTableContent(String fromMethod)
	throws SQLException
	{
		boolean doPrint = false;
		if ( ! doPrint )
			return;
			
		String sql = "SELECT * from [DiskSpace]";
		sql = dbxConn.quotifySqlString(sql);

		try (Statement stmt = dbxConn.createStatement(); ResultSet rs = stmt.executeQuery(sql))
		{
			ResultSetTableModel rstm = new ResultSetTableModel(rs, sql);
			System.out.println("################## " + fromMethod);
			System.out.println(rstm.toAsciiTableString());
		}
	}
	/**
	 * Test basic linear growth prediction
	 */
	@Test
	public void testLinearGrowth() throws SQLException
	{
		// Create test table
		createDiskSpaceTable();

		// Insert data: 1GB disk, growing 10MB/hour for 48 hours
		LocalDateTime now = LocalDateTime.now();
		long totalKB = 1024 * 1024; // 1GB
		long startUsedKB = 100 * 1024; // Start at 100MB

		try (Statement stmt = dbxConn.createStatement())
		{
			for (int hour = 0; hour < 48; hour++)
			{
				long usedKB = startUsedKB + (hour * 10 * 1024); // +10MB per hour
				long freeKB = totalKB - usedKB;
				Timestamp ts = Timestamp.valueOf(now.minusHours(48 - hour));

				String sql = String.format(
					"INSERT INTO [DiskSpace] VALUES ('/data', '%s', %d, %d, %d)",
					ts, totalKB, usedKB, freeKB
				);
				sql = dbxConn.quotifySqlString(sql);
				stmt.executeUpdate(sql);
			}
		}
		printTableContent("testLinearGrowth");
		
		// Run prediction
		Set<PredictionResult> results = predictor.predictSpaceFull(
			SourceDataSize.KB,
			"MountPoint",
			null,
			"SampleTime",
			"TotalKB",
			"UsedKB",
			"FreeKB",
			null,
			"DiskSpace"
		);

		assertEquals(1, results.size());
		PredictionResult result = results.iterator().next();

		// Verify predictions
		assertTrue(result._hasSufficientData);
		assertEquals("/data", result._resourceName);
		assertEquals(48, result._dataPointsUsed);

		// Growth should be ~10MB/day * 24 = 240MB/day
		assertTrue("Growth rate should be ~240 MB/day", 
			Math.abs(result._growthRateMBPerDay - 240.0) < 5.0);

		// Should predict running out of space
		assertTrue("Should predict depletion", result._hoursUntilFull > 0);
		
		// R² should be very high for perfect linear data
		assertTrue("R² should be > 0.99 for linear data", result._rSquared > 0.99);
	}

	/**
	 * Test handling of peaks (daily spikes)
	 */
	@Test
	public void testPeakFiltering() throws SQLException
	{
		createDiskSpaceTable();

		LocalDateTime now = LocalDateTime.now();
		long totalKB = 1024 * 1024; // 1GB
		long baseUsedKB = 200 * 1024; // 200MB baseline

		try (Statement stmt = dbxConn.createStatement())
		{
			for (int hour = 0; hour < 72; hour++)
			{
				long usedKB = baseUsedKB + (hour * 5 * 1024); // Base growth: 5MB/hour
				
				// Add spike every 24 hours (simulating daily backup)
				if (hour % 24 == 12)
				{
					usedKB += 100 * 1024; // +100MB spike
				}

				long freeKB = totalKB - usedKB;
				Timestamp ts = Timestamp.valueOf(now.minusHours(72 - hour));

				String sql = String.format(
					"INSERT INTO [DiskSpace] VALUES ('/backup', '%s', %d, %d, %d)",
					ts, totalKB, usedKB, freeKB
				);
				sql = dbxConn.quotifySqlString(sql);
				stmt.executeUpdate(sql);
			}
		}
		printTableContent("testPeakFiltering");

		// Use 75th percentile to filter peaks
		predictor.setPercentileFilter(75);

		Set<PredictionResult> results = predictor.predictSpaceFull(
			SourceDataSize.KB,
			"MountPoint",
			"",
			"SampleTime",
			"TotalKB",
			"UsedKB",
			"FreeKB",
			null,
			"DiskSpace"
		);

		PredictionResult result = results.iterator().next();

		// Growth should be closer to base rate (120 MB/day) than if peaks were included
		// If peaks weren't filtered, it would be higher
		assertTrue("Filtered growth should be ~120 MB/day", 
			Math.abs(result._growthRateMBPerDay - 120.0) < 20.0);
	}

	/**
	 * Test stable/decreasing usage
	 */
	@Test
	public void testStableUsage() throws SQLException
	{
		createDiskSpaceTable();

		LocalDateTime now = LocalDateTime.now();
		long totalKB = 1024 * 1024;
		long usedKB = 500 * 1024; // Constant 500MB

		try (Statement stmt = dbxConn.createStatement())
		{
			for (int hour = 0; hour < 48; hour++)
			{
				long freeKB = totalKB - usedKB;
				Timestamp ts = Timestamp.valueOf(now.minusHours(48 - hour));

				String sql = String.format(
					"INSERT INTO [DiskSpace] VALUES ('/stable', '%s', %d, %d, %d)",
					ts, totalKB, usedKB, freeKB
				);
				sql = dbxConn.quotifySqlString(sql);
				stmt.executeUpdate(sql);
			}
		}
		printTableContent("testStableUsage");

		Set<PredictionResult> results = predictor.predictSpaceFull(
			SourceDataSize.KB,
			"MountPoint",
			"",
			"SampleTime",
			"TotalKB",
			"UsedKB",
			"FreeKB",
			null,
			"DiskSpace"
		);

		PredictionResult result = results.iterator().next();

		// Growth should be ~0
		assertTrue("Growth should be near zero", 
			Math.abs(result._growthRateMBPerDay) < 1.0);
		
		// Should be marked as OK (not filling up)
		assertEquals("OK", result._warningLevel);
	}

	/**
	 * Test insufficient data handling
	 */
	@Test
	public void testInsufficientData() throws SQLException
	{
		createDiskSpaceTable();

		LocalDateTime now = LocalDateTime.now();
		long totalKB = 1024 * 1024;

		// Insert only 5 data points (need 10 minimum)
		try (Statement stmt = dbxConn.createStatement())
		{
			for (int hour = 0; hour < 5; hour++)
			{
				long usedKB = 100 * 1024 + (hour * 10 * 1024);
				long freeKB = totalKB - usedKB;
				Timestamp ts = Timestamp.valueOf(now.minusHours(5 - hour));

				String sql = String.format(
					"INSERT INTO [DiskSpace] VALUES ('/small', '%s', %d, %d, %d)",
					ts, totalKB, usedKB, freeKB
				);
				sql = dbxConn.quotifySqlString(sql);
				stmt.executeUpdate(sql);
			}
		}
		printTableContent("testInsufficientData");

		Set<PredictionResult> results = predictor.predictSpaceFull(
			SourceDataSize.KB,
			"MountPoint",
			"",
			"SampleTime",
			"TotalKB",
			"UsedKB",
			"FreeKB",
			null,
			"DiskSpace"
		);

		PredictionResult result = results.iterator().next();

		assertFalse("Should not have sufficient data", result._hasSufficientData);
		assertEquals(5, result._dataPointsUsed);
	}

	/**
	 * Test MB to KB conversion
	 */
	@Test
	public void testMBtoKBConversion() throws SQLException
	{
		try (Statement stmt = dbxConn.createStatement())
		{
			String sql = "DROP TABLE IF EXISTS [DiskSpaceMB]";

			sql = dbxConn.quotifySqlString(sql);
			stmt.execute(sql);
		}

		// Create table with MB columns
		try (Statement stmt = dbxConn.createStatement())
		{
			String sql = ""
				+ "CREATE TABLE IF NOT EXISTS [DiskSpaceMB] \n"
				+ "( \n"
				+ "   [MountPoint] VARCHAR(100) \n"
				+ "  ,[SampleTime] TIMESTAMP \n"
				+ "  ,[TotalMB]    BIGINT \n"
				+ "  ,[UsedMB]     BIGINT \n"
				+ "  ,[FreeMB]     BIGINT \n"
				+ ") \n"
				;

			sql = dbxConn.quotifySqlString(sql);
			stmt.execute(sql);
		}

		LocalDateTime now = LocalDateTime.now();

		try (Statement stmt = dbxConn.createStatement())
		{
			for (int hour = 0; hour < 24; hour++)
			{
				long totalMB = 1024; // 1GB
				long usedMB = 100 + (hour * 10); // +10MB per hour
				long freeMB = totalMB - usedMB;
				Timestamp ts = Timestamp.valueOf(now.minusHours(24 - hour));

				String sql = String.format(
					"INSERT INTO [DiskSpaceMB] VALUES ('/data', '%s', %d, %d, %d)",
					ts, totalMB, usedMB, freeMB
				);
				sql = dbxConn.quotifySqlString(sql);
				stmt.executeUpdate(sql);
			}
		}
		printTableContent("testMBtoKBConversion");

		// Use MB source data size
		Set<PredictionResult> results = predictor.predictSpaceFull(
			SourceDataSize.MB, // <-- MB instead of KB
			"MountPoint",
			"",
			"SampleTime",
			"TotalMB",
			"UsedMB",
			"FreeMB",
			null,
			"DiskSpaceMB"
		);

		PredictionResult result = results.iterator().next();

		assertTrue(result._hasSufficientData);
		
		// Values should be converted to KB internally
		double oneGb = 1024.0d * 1024.0d;
		assertEquals(oneGb, result._totalSizeKB, 0d); // 1GB in KB
		
		// Growth should be ~240 MB/day
		assertTrue("Growth ~240 MB/day", 
			Math.abs(result._growthRateMBPerDay - 240.0) < 5.0);
	}

	/**
	 * Test multiple resources in same query
	 */
	@Test
	public void testMultipleResources() throws SQLException
	{
		createDiskSpaceTable();

		LocalDateTime now = LocalDateTime.now();

		try (Statement stmt = dbxConn.createStatement())
		{
			// Insert data for 3 different mount points
			for (String mount : new String[]{"/data", "/backup", "/logs"})
			{
				long totalKB = 1024 * 1024;
				long startUsedKB = 200 * 1024;

				for (int hour = 0; hour < 48; hour++)
				{
					long usedKB = startUsedKB + (hour * 5 * 1024);
					long freeKB = totalKB - usedKB;
					Timestamp ts = Timestamp.valueOf(now.minusHours(48 - hour));

					String sql = String.format(
						"INSERT INTO [DiskSpace] VALUES ('%s', '%s', %d, %d, %d)",
						mount, ts, totalKB, usedKB, freeKB
					);
					sql = dbxConn.quotifySqlString(sql);
					stmt.executeUpdate(sql);
				}
			}
		}
		printTableContent("testMultipleResources");

		Set<PredictionResult> results = predictor.predictSpaceFull(
			SourceDataSize.KB,
			"MountPoint",
			null,
			"SampleTime",
			"TotalKB",
			"UsedKB",
			"FreeKB",
			null,
			"DiskSpace"
		);

		assertEquals("Should have 3 results", 3, results.size());

		for (PredictionResult result : results)
		{
			assertTrue(result._hasSufficientData);
			assertEquals(48, result._dataPointsUsed);
		}
	}

	/**
	 * Test warning level thresholds
	 */
	@Test
	public void testWarningLevels() throws SQLException
	{
		createDiskSpaceTable();

		// Configure thresholds
		predictor.setCriticalThresholdHours(24);  // < 24 hours = CRITICAL
		predictor.setWarningThresholdHours(72);   // < 72 hours = WARNING

		LocalDateTime now = LocalDateTime.now();
		long totalKB = 1024 * 1024;

		try (Statement stmt = dbxConn.createStatement())
		{
			// Rapid growth: will fill in ~10 hours
			for (int hour = 0; hour < 24; hour++)
			{
				long usedKB = 500 * 1024 + (hour * 50 * 1024); // +50MB/hour
				long freeKB = totalKB - usedKB;
				Timestamp ts = Timestamp.valueOf(now.minusHours(24 - hour));

				String sql = String.format(
					"INSERT INTO [DiskSpace] VALUES ('/critical', '%s', %d, %d, %d)",
					ts, totalKB, usedKB, freeKB
				);
				sql = dbxConn.quotifySqlString(sql);
				stmt.executeUpdate(sql);
			}
		}
		printTableContent("testWarningLevels");

		Set<PredictionResult> results = predictor.predictSpaceFull(
			SourceDataSize.KB,
			"MountPoint",
			"MountPoint",
			"SampleTime",
			"TotalKB",
			"UsedKB",
			"FreeKB",
			null,
			"DiskSpace"
		);

		PredictionResult result = results.iterator().next();

		assertEquals("CRITICAL", result._warningLevel);
		assertTrue("Should fill in < 24 hours", result._hoursUntilFull < 24);
	}

	/**
	 * Test HTML report generation
	 */
	@Test
	public void testHtmlReportGeneration() throws SQLException
	{
		createDiskSpaceTable();

		LocalDateTime now = LocalDateTime.now();
		long totalKB = 1024 * 1024;

		try (Statement stmt = dbxConn.createStatement())
		{
			for (int hour = 0; hour < 24; hour++)
			{
				long usedKB = 100 * 1024 + (hour * 10 * 1024);
				long freeKB = totalKB - usedKB;
				Timestamp ts = Timestamp.valueOf(now.minusHours(24 - hour));

				String sql = String.format(
					"INSERT INTO [DiskSpace] VALUES ('/data', '%s', %d, %d, %d)",
					ts, totalKB, usedKB, freeKB
				);
				sql = dbxConn.quotifySqlString(sql);
				stmt.executeUpdate(sql);
			}
		}
		printTableContent("testHtmlReportGeneration");

		Set<PredictionResult> results = predictor.predictSpaceFull(
			SourceDataSize.KB,
			"MountPoint",
			null,
			"SampleTime",
			"TotalKB",
			"UsedKB",
			"FreeKB",
			null,
			"DiskSpace"
		);

		String htmlFull = predictor.generateHtmlReport(results, true, true, null);
		String htmlPartial = predictor.generateHtmlReport(results, false, true, null);

		// Full page should have DOCTYPE and html tags
		assertTrue(htmlFull.contains("<!DOCTYPE html>"));
		assertTrue(htmlFull.contains("<html>"));
		assertTrue(htmlFull.contains("</html>"));

		// Partial should not
		assertFalse(htmlPartial.contains("<!DOCTYPE html>"));
		assertFalse(htmlPartial.contains("<html>"));

		// Both should have the table
		assertTrue(htmlFull.contains("size-predict"));
		assertTrue(htmlPartial.contains("size-predict"));
	}

	/**
	 * Helper: Create test table
	 */
	private void createDiskSpaceTable() throws SQLException
	{
		try (Statement stmt = dbxConn.createStatement())
		{
			String sql = "DROP TABLE IF EXISTS [DiskSpace]";

			sql = dbxConn.quotifySqlString(sql);
			stmt.execute(sql);
		}

		try (Statement stmt = dbxConn.createStatement())
		{
			String sql = ""
					+ "CREATE TABLE [DiskSpace] \n"
					+ "( \n"
					+ "   [MountPoint] VARCHAR(100) \n"
					+ "  ,[SampleTime] TIMESTAMP \n"
					+ "  ,[TotalKB]    BIGINT \n"
					+ "  ,[UsedKB]     BIGINT \n"
					+ "  ,[FreeKB]     BIGINT \n"
					+ ") \n"
					;

			sql = dbxConn.quotifySqlString(sql);
			stmt.execute(sql);
		}
	}
}
