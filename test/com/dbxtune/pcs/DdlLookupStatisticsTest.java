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
package com.dbxtune.pcs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;

import com.dbxtune.pcs.DdlLookupStatistics.Counter;

public class DdlLookupStatisticsTest
{
	private static LocalDateTime time(int day, int hour, int minute)
	{
		return LocalDateTime.of(2026, 11, day, hour, minute, 0);
	}

	@Test
	public void testIssuesAreCountedAsTimeoutOrError()
	{
		DdlLookupStatistics stat = new DdlLookupStatistics(time(4, 14, 23));
		stat.addIssue(new SQLTimeoutException("timeout"));
		stat.addIssue(new SQLException("JZ006: Caught IOException: java.io.IOException: JZ0T3: Read operation timed out.", "JZ006"));
		stat.addIssue(new SQLException("JZ006: Caught IOException: java.net.SocketTimeoutException: Read timed out.", "JZ006"));
		stat.addIssue(new SQLException("The query has timed out.", "HY008"));
		stat.addIssue(new SQLException("JZ006: Caught IOException: com.sybase.jdbc4.jdbc.SybConnectionDeadException: JZ0C0: Connection is already closed.", "JZ006"));
		stat.addIssue(new SQLException("Incorrect syntax near 'master'.", "ZZZZZ", 102));
		stat.addIssue(null);

		String line = stat.rollOver(time(4, 15, 0), true, false, false).get(0);
		assertTrue(line, line.contains(", queryTimeouts=4, queryErrors=2"));
	}

	@Test
	public void testTimeOfALookupGoesToTheWorstThatHappenedInIt()
	{
		DdlLookupStatistics stat = new DdlLookupStatistics(time(4, 14, 23));

		stat.beginLookup();
		stat.addLookup(30, 1);

		stat.beginLookup();
		stat.addIssue(new SQLException("Some error"));
		stat.addLookup(5, 1);

		stat.beginLookup();
		stat.addIssue(new SQLException("Some error"));
		stat.addIssue(new SQLException("The query has timed out.", "HY008"));
		stat.addIssue(new SQLException("Some error"));
		stat.addLookup(10000, 1);

		// Nothing left from the lookup before
		stat.beginLookup();
		stat.addLookup(50, 1);

		String line = stat.rollOver(time(4, 15, 0), true, false, false).get(0);
		assertTrue(line, line.contains(", lookups=4,"));
		assertTrue(line, line.contains(", lookupsOk=2, okTimeMs=80, okMaxMs=50, okAvgMs=40, lookupsTimeout=1, timeoutTimeMs=10000, lookupsError=1, errorTimeMs=5,"));
		assertTrue(line, line.endsWith(", queryTimeouts=1, queryErrors=3"));
	}

	@Test
	public void testNothingPrintedWithinTheSameHour()
	{
		DdlLookupStatistics stat = new DdlLookupStatistics(time(4, 14, 23));
		stat.inc(Counter.requests);

		assertTrue(stat.rollOver(time(4, 14, 59), true, true, false).isEmpty());
	}

	@Test
	public void testHourLineThenCountersStartOver()
	{
		DdlLookupStatistics stat = new DdlLookupStatistics(time(4, 14, 23));
		stat.inc(Counter.requests);
		stat.inc(Counter.requests);
		stat.inc(Counter.alreadyStored);
		stat.addLookup(10, 1);
		stat.addLookup(30, 0);
		stat.inc(Counter.saved);

		List<String> lines = stat.rollOver(time(4, 15, 0), true, true, false);
		assertEquals(1, lines.size());
		assertEquals("DDL Lookup Statistics HOUR [2026-11-04 14:23:00 - 2026-11-04 15:00:00]:"
				+ " requests=2, alreadyQueued=0, alreadyStored=1, notAllowed=0, lookups=2, lookupsNoResult=1, objectsReturned=1"
				+ ", saved=1, notSaved=0, lookupsOk=2, okTimeMs=40, okMaxMs=30, okAvgMs=20, lookupsTimeout=0, timeoutTimeMs=0, lookupsError=0, errorTimeMs=0, metadataSql=0, metadataSpHelp=0, metadataFallback=0, queryTimeouts=0, queryErrors=0", lines.get(0));

		lines = stat.rollOver(time(4, 16, 0), true, true, false);
		assertEquals(1, lines.size());
		assertEquals("DDL Lookup Statistics HOUR [2026-11-04 15:00:00 - 2026-11-04 16:00:00]:"
				+ " requests=0, alreadyQueued=0, alreadyStored=0, notAllowed=0, lookups=0, lookupsNoResult=0, objectsReturned=0"
				+ ", saved=0, notSaved=0, lookupsOk=0, okTimeMs=0, okMaxMs=0, okAvgMs=0, lookupsTimeout=0, timeoutTimeMs=0, lookupsError=0, errorTimeMs=0, metadataSql=0, metadataSpHelp=0, metadataFallback=0, queryTimeouts=0, queryErrors=0", lines.get(0));
	}

	@Test
	public void testNewDayPrintsLastHourAndSummaryOfTheDay()
	{
		DdlLookupStatistics stat = new DdlLookupStatistics(time(4, 22, 10));
		stat.inc(Counter.requests);
		stat.addLookup(50, 1);
		assertEquals(1, stat.rollOver(time(4, 23, 0), true, true, false).size());

		stat.inc(Counter.requests);
		stat.inc(Counter.requests);
		stat.addLookup(20, 2);

		List<String> lines = stat.rollOver(time(5, 0, 0), true, true, false);
		assertEquals(2, lines.size());
		assertTrue(lines.get(0), lines.get(0).startsWith("DDL Lookup Statistics HOUR [2026-11-04 23:00:00 - 2026-11-05 00:00:00]: requests=2,"));
		assertEquals("DDL Lookup Statistics DAY [2026-11-04 22:10:00 - 2026-11-05 00:00:00]:"
				+ " requests=3, alreadyQueued=0, alreadyStored=0, notAllowed=0, lookups=2, lookupsNoResult=0, objectsReturned=3"
				+ ", saved=0, notSaved=0, lookupsOk=2, okTimeMs=70, okMaxMs=50, okAvgMs=35, lookupsTimeout=0, timeoutTimeMs=0, lookupsError=0, errorTimeMs=0, metadataSql=0, metadataSpHelp=0, metadataFallback=0, queryTimeouts=0, queryErrors=0", lines.get(1));

		// The new day starts from zero
		stat.inc(Counter.requests);
		lines = stat.rollOver(time(6, 0, 0), false, true, false);
		assertEquals(1, lines.size());
		assertTrue(lines.get(0), lines.get(0).startsWith("DDL Lookup Statistics DAY [2026-11-05 00:00:00 - 2026-11-06 00:00:00]: requests=1,"));
	}

	@Test
	public void testHourlyOffStillCountsInTheDay()
	{
		DdlLookupStatistics stat = new DdlLookupStatistics(time(4, 22, 0));
		stat.inc(Counter.requests);
		assertTrue(stat.rollOver(time(4, 23, 0), false, true, false).isEmpty());

		stat.inc(Counter.requests);
		List<String> lines = stat.rollOver(time(5, 0, 0), false, true, false);
		assertEquals(1, lines.size());
		assertTrue(lines.get(0), lines.get(0).startsWith("DDL Lookup Statistics DAY [2026-11-04 22:00:00 - 2026-11-05 00:00:00]: requests=2,"));
	}

	@Test
	public void testAllOffPrintsNothing()
	{
		DdlLookupStatistics stat = new DdlLookupStatistics(time(4, 23, 0));
		stat.inc(Counter.requests);
		assertTrue(stat.rollOver(time(5, 0, 0), false, false, false).isEmpty());
	}

	@Test
	public void testShutdownPrintsTheLastPart()
	{
		DdlLookupStatistics stat = new DdlLookupStatistics(time(4, 14, 23));
		stat.inc(Counter.requests);

		List<String> lines = stat.rollOver(time(4, 14, 40), true, true, true);
		assertEquals(2, lines.size());
		assertTrue(lines.get(0), lines.get(0).startsWith("DDL Lookup Statistics HOUR [2026-11-04 14:23:00 - 2026-11-04 14:40:00]: requests=1,"));
		assertTrue(lines.get(1), lines.get(1).startsWith("DDL Lookup Statistics DAY [2026-11-04 14:23:00 - 2026-11-04 14:40:00]: requests=1,"));
	}
}
