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

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLongArray;

import com.dbxtune.utils.DbUtils;

/**
 * Counters for the DDL Lookup in PersistentCounterHandler, printed as one line in the errorlog for every hour and every day.
 * <p>
 * The counters are incremented by several threads, rollOver() is called by the DDL Lookup thread only.
 */
class DdlLookupStatistics
{
	enum Counter
	{
		/** addDdl() was called */
		requests,

		/** Discarded: already in the input queue */
		alreadyQueued,

		/** Discarded: already stored, or marked as discarded, by the writers */
		alreadyStored,

		/** Discarded: the inspector or the configuration did not allow it */
		notAllowed,

		/** The lookup was executed on the monitored server */
		lookups,

		/** The lookup was executed, but found nothing to store */
		lookupsNoResult,

		/** Number of objects found by the lookups (one lookup can find several, or none) */
		objectsReturned,

		/** An object was written to the DDL Storage */
		saved,

		/** An object was NOT written to the DDL Storage: already there under another name, no connection, or an error */
		notSaved,

		/** A lookup where all queries worked */
		lookupsOk,

		/** Time in the lookups that worked, at the client side */
		okTimeMs,

		/** Slowest lookup that worked */
		okMaxMs,

		/** A lookup where at least one query timed out */
		lookupsTimeout,

		/** Time in the lookups with a timeout, the whole lookup */
		timeoutTimeMs,

		/** A lookup where at least one query failed, and none timed out */
		lookupsError,

		/** Time in the lookups with an error, the whole lookup */
		errorTimeMs,

		/** Table metadata was fetched with plain SQL */
		metadataSql,

		/** Table metadata was fetched with sp_help, since plain SQL isn't used for this server or table */
		metadataSpHelp,

		/** Table metadata was fetched with sp_help, since plain SQL failed */
		metadataFallback,

		/** A query in a lookup timed out. Counted per query: one lookup runs several */
		queryTimeouts,

		/** A query in a lookup failed for another reason than a timeout. Counted per query: one lookup runs several */
		queryErrors;
	}

	private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final AtomicLongArray _hour = new AtomicLongArray(Counter.values().length);
	private final long[]          _day  = new long[Counter.values().length];

	private LocalDateTime _hourStart;
	private LocalDateTime _dayStart;

	// What happened in the current lookup. Only touched by the DDL Lookup thread
	private boolean _lookupHasTimeout;
	private boolean _lookupHasError;

	DdlLookupStatistics(LocalDateTime now)
	{
		_hourStart = now;
		_dayStart  = now;
	}

	void inc(Counter counter)
	{
		add(counter, 1);
	}

	void add(Counter counter, long value)
	{
		_hour.addAndGet(counter.ordinal(), value);
	}

	/** A lookup starts */
	void beginLookup()
	{
		_lookupHasTimeout = false;
		_lookupHasError   = false;
	}

	/** A lookup was executed, and found this number of objects. The time goes to the worst thing that happened in it, so a timeout doesn't change the average of those that worked */
	void addLookup(long timeMs, int objectCount)
	{
		inc(Counter.lookups);
		add(Counter.objectsReturned, objectCount);
		if (objectCount == 0)
			inc(Counter.lookupsNoResult);

		if (_lookupHasTimeout)
		{
			inc(Counter.lookupsTimeout);
			add(Counter.timeoutTimeMs, timeMs);
		}
		else if (_lookupHasError)
		{
			inc(Counter.lookupsError);
			add(Counter.errorTimeMs, timeMs);
		}
		else
		{
			inc(Counter.lookupsOk);
			add(Counter.okTimeMs, timeMs);
			_hour.accumulateAndGet(Counter.okMaxMs.ordinal(), timeMs, Math::max);
		}
	}

	/** A query in a lookup failed. null = nothing failed */
	void addIssue(SQLException ex)
	{
		if (ex == null)
			return;

		if (DbUtils.isQueryTimeout(ex))
		{
			inc(Counter.queryTimeouts);
			_lookupHasTimeout = true;
		}
		else
		{
			inc(Counter.queryErrors);
			_lookupHasError = true;
		}
	}

	/**
	 * Check if we have reached a new hour or a new day.
	 *
	 * @param now         Current time
	 * @param printHour   Add a line for the hour that has ended
	 * @param printDay    Add a line for the day that has ended, the summary of its hours
	 * @param isShutdown  Nothing more will be counted: end both periods now, so the last part isn't lost
	 * @return The lines to print. Empty if no period has ended
	 */
	List<String> rollOver(LocalDateTime now, boolean printHour, boolean printDay, boolean isShutdown)
	{
		List<String> lines = new ArrayList<>();

		LocalDateTime hourNow = now.truncatedTo(ChronoUnit.HOURS);
		if ( ! isShutdown && hourNow.equals(_hourStart.truncatedTo(ChronoUnit.HOURS)) )
			return lines;

		LocalDateTime periodEnd = isShutdown ? now : hourNow;

		long[] hour = new long[_day.length];
		for (int i = 0; i < hour.length; i++)
		{
			hour[i] = _hour.getAndSet(i, 0);
			_day[i] = i == Counter.okMaxMs.ordinal() ? Math.max(_day[i], hour[i]) : _day[i] + hour[i];
		}

		if (printHour)
			lines.add(toLine("HOUR", _hourStart, periodEnd, hour));
		_hourStart = periodEnd;

		if (isShutdown || ! now.toLocalDate().equals(_dayStart.toLocalDate()))
		{
			if (printDay)
				lines.add(toLine("DAY", _dayStart, periodEnd, _day));
			Arrays.fill(_day, 0);
			_dayStart = periodEnd;
		}
		return lines;
	}

	private static String toLine(String periodName, LocalDateTime start, LocalDateTime end, long[] values)
	{
		StringBuilder sb = new StringBuilder("DDL Lookup Statistics ").append(periodName);
		sb.append(" [").append(FORMAT.format(start)).append(" - ").append(FORMAT.format(end)).append("]:");

		String comma = " ";
		for (Counter counter : Counter.values())
		{
			sb.append(comma).append(counter).append("=").append(values[counter.ordinal()]);
			comma = ", ";

			if (counter == Counter.okMaxMs)
			{
				long lookupsOk = values[Counter.lookupsOk.ordinal()];
				sb.append(", okAvgMs=").append(lookupsOk == 0 ? 0 : values[Counter.okTimeMs.ordinal()] / lookupsOk);
			}
		}
		return sb.toString();
	}
}
