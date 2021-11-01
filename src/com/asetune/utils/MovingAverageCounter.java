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
package com.asetune.utils;

import java.util.LinkedList;

public class MovingAverageCounter
{
	private String _name;
	private int    _minutes;
	
	private boolean _isPeriodAchieved;

	private LinkedList<Entry> _values = new LinkedList<>();

	public MovingAverageCounter(String name, int minutes)
	throws RuntimeException
	{
		if (name == null)   throw new RuntimeException("Name can't be null");
		if (name.isEmpty()) throw new RuntimeException("Name can't be '' (blank or empty string)");
		if (minutes < 1)    throw new RuntimeException("Minute specification must be 1 or higher");
		
		_name    = name;
		_minutes = minutes;
		_isPeriodAchieved = false;
	}

	/**
	 * Create a String to be used as a key in various maps...
	 * 
	 * @param name
	 * @param minutes
	 * @return name + ":" + minutes
	 */
	public static String createName(String name, int minutes)
	{
		return name + ":" + minutes;
	}

	/** Get key (or name:minutes) of this entry */
	public String getKey()
	{
		return createName(_name, _minutes);
	}

	/** get name of the entry */
	public String getName()
	{
		return _name;
	}

	/** get how many minutes does this counter hold */
	public int getSampleMinutes()
	{
		return _minutes;
	}

	/** Returns the number of values in the period. */
	public int size()
	{
		// first remove old entries
		removeOldEntries();

		return _values.size();
	}

	/** reset the list of values and set _isPeriodAchieved=false */
	public void reset()
	{
		_isPeriodAchieved = false;
		_values.clear();
	}

	/** get milliseconds for the oldest entry */
	public synchronized long getOldestAgeInMs()
	{
		// first remove old entries
		removeOldEntries();

		if (_values.isEmpty())
			return -1;

		Entry oldestEntry = _values.getFirst(); // note: new entries are added at "END", so oldest entry would be FIRST
		long now = System.currentTimeMillis();
		long ageinMs = now - oldestEntry._ts;

		return ageinMs;
	}

	/** get seconds for the oldest entry */
	public int getOldestAgeInSec()
	{
		if (_values.isEmpty())
			return -1;

		return (int) getOldestAgeInMs() / 1000;
	}

	/**
	 * has time objective been Achieved (has we been samples for a longer time than X minutes)
	 * @return 
	 */
	public boolean isPeriodAchieved()
	{
		// first remove old entries
		removeOldEntries();

		return _isPeriodAchieved;
	}
	
	/**
	 * Remove entries that's no longer in the sample period
	 */
	private synchronized void removeOldEntries()
	{
		if (_values.isEmpty())
			return;

		long now = System.currentTimeMillis();
		long threshold = _minutes * 60 * 1000;
		while(true)
		{
			Entry oldestEntry = _values.getFirst(); // note: new entries are added at "END", so oldest entry would be FIRST
			long ageinMs = now - oldestEntry._ts;
			if (ageinMs > threshold)
			{
				_values.removeFirst(); // oldest entry would be FIRST
				_isPeriodAchieved = true;
			}
			else
				break;
		}
	}

	/**
	 * Add a entry
	 * 
	 * @param val
	 * @return this object so we easily can call <code>getAvg</code> on a <i>one-liner</i>
	 */
	public synchronized MovingAverageCounter add(double val)
	{
		// first remove old entries
		removeOldEntries();

		// Add entry 
		// note: new entries are added at "END", so oldest entry would be FIRST
		_values.add(new Entry(System.currentTimeMillis(), val));
		
		return this;
	}

	/**
	 * Get average value for records that has been sampled during the sample period
	 * <ul>
	 *   <li>If the period has no values, ZERO will be returned </li>
	 *   <li>if the sample period has <b>NOT</b> been achieved, average will still be calculated on added values.</li>
	 * </ul>
	 * 
	 * To check if the sample period has been achieved call <code>isPeriodAchieved()</code>
	 */
	public double getAvg()
	{
		return getAvg(0d, false);
	}

	/**
	 * Get average value for records that has been sampled during the sample period
	 * <ul>
	 *   <li>If the period has no values, <i>defaultVal</i> will be returned </li>
	 *   <li>if the sample period has <b>NOT</b> been achieved, average will still be calculated on added values.</li>
	 * </ul>
	 * 
	 * To check if the sample period has been achieved call <code>isPeriodAchieved()</code>
	 * 
	 * @param defaultVal                                  default value
	 */
	public double getAvg(double defaultVal)
	{
		return getAvg(defaultVal, false);
	}

	/**
	 * Get average value for records that has been sampled during the sample period
	 * <ul>
	 *   <li>If the period has no values, <i>defaultVal</i> will be returned </li>
	 *   <li>if the sample period has <b>NOT</b> been achieved, average or <i>defaultVal</i> will be returned. based on parameter: <code>returnDefaultIfPeriodHasNotBeenAchieved</code></li>
	 * </ul>
	 * 
	 * To check if the sample period has been achieved call <code>isPeriodAchieved()</code>
	 * 
	 * @param defaultVal                                  default value
	 * @param returnDefaultIfPeriodHasNotBeenAchieved     like the name of the parameter
	 */
	public synchronized double getAvg(double defaultVal, boolean returnDefaultIfPeriodHasNotBeenAchieved)
	{
		// first remove old entries
		removeOldEntries();
		
		int size = _values.size();
		double sum = 0;

		if (size == 0)
			return defaultVal;

		if ( returnDefaultIfPeriodHasNotBeenAchieved  &&  ! isPeriodAchieved() )
			return defaultVal;
			
		for (Entry e : _values)
		{
			sum += e._val;
		}
		
		return sum / size;
	}

//	/**
//	 * Get average value for records that has been sampled during the sample period
//	 * 
//	 * @param returnNullIfPeriodHasNotBeenAchieved  What the parameter name says...
//	 * @return
//	 */
//	public synchronized Double getAvg(boolean returnNullIfPeriodHasNotBeenAchieved)
//	{
//		if (returnNullIfPeriodHasNotBeenAchieved  &&  ! isPeriodAchieved() )
//			return null;
//
//		return getAvg();
//	}
	
	/**
	 * Simple place holder entry
	 */
	private static class Entry
	{
		long   _ts;
		double _val;

		public Entry(long ts, double val)
		{
			_ts  = ts;
			_val = val;
		}
	}
	
}