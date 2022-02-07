/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.utils;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * @author qschgor
 *
 */
public class TimeUtils
{
	/** synchronize on this object before it's used... SimpleDateFormat is <b>NOT</b> thread safe */
	public static final SimpleDateFormat ISO8601_DATE_FORMAT;
	public static final SimpleDateFormat         DATE_FORMAT;

	public static final SimpleDateFormat ISO8601_UTC_DATE_FORMAT;
	public static final SimpleDateFormat         UTC_DATE_FORMAT;

	static
	{
		// Initialize the LOCAL TIMEZONE 
		ISO8601_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
		        DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		
		// Initialize the UTC TIMEZONE 
		ISO8601_UTC_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
		        UTC_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

		ISO8601_UTC_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
		        UTC_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	/*---------------------------------------------------
	** class members
	**---------------------------------------------------
	*/
	private static HashMap<String, Long> _timeExpiredMap = new HashMap<String, Long>();   

	/*---------------------------------------------------
	** Constructors
	**---------------------------------------------------
	*/

	/*---------------------------------------------------
	** Methods: local to class
	**---------------------------------------------------
	*/

	
	/**
	 * Ask if we should print a message<br>
	 * Or basically if <code>timeLimitInMs</code> has passed since last call with the same <code>key</code><br>
	 * If we call this many times
	 * <ul>
	 *   <li>First time it will always return true.</li>
	 *   <li>Second time will it return</li>
	 *   <ul>
	 *     <li>true if it has passed <code>timeLimitInMs</code> since last call</li>
	 *     <li>false if not enough time has passed since last message</li>
	 *   </ul>
	 * </ul>
	 * @param key              Just a string that will identify this specific "event"
	 * @param timeLimitInMs    Time limit since last call (return true|false)
	 * @return true if first time, or if the time limit has passed since last call
	 */
	public static boolean timeExpired(String key, long timeLimitInMs)
	{
		// Get previous start time
		Long lastCallTime = _timeExpiredMap.get(key);

		// If not found... print message and store the time
		if (lastCallTime == null)
		{
			lastCallTime = new Long(System.currentTimeMillis()); 
			_timeExpiredMap.put(key, lastCallTime);
			return true;
		}
			
		// if "over" the timeLimit... print message and store the "new" time
		if (System.currentTimeMillis() - lastCallTime > timeLimitInMs)
		{
			lastCallTime = new Long(System.currentTimeMillis()); 
			_timeExpiredMap.put(key, lastCallTime);
			return true;
		}
		return false;
	}

	/** @see timeExpired */
	public static boolean printMessage(String key, long timeLimitInMs)
	{
		return timeExpired(key, timeLimitInMs);
	}

	/**
	 * Set a new start time for method timeExpired
	 * @param key              Just a string that will identify this specific "event"
	 */
	public static void timeExpiredSetStartTime(String key)
	{
		_timeExpiredMap.put( key, new Long(System.currentTimeMillis()) );
	}
	
//	public static boolean printMessage(String key, long timeLimitInMs, String messageToPrint, boolean appendDiscardCount)
//	{
//	}

	/** simply does: System.currentTimeMillis() - startTime; */
	public static long msDiffNow(long startTime)
	{
		return System.currentTimeMillis() - startTime;
	}

	/** simply call msDiffNow(startTime); return msToTimeStr(execTime); */
	public static String msDiffNowToTimeStr(long startTime)
	{
		long execTime = msDiffNow(startTime);
		return msToTimeStr(execTime);
	}
	
	/** simply call msDiffNow(startTime); msToTimeStr(format, execTime); */
	public static String msDiffNowToTimeStr(String format, long startTime)
	{
		long execTime = msDiffNow(startTime);
		return msToTimeStr(format, execTime);
	}

	/**
	 * Convert a long into a time string HH:MM:SS.ms
	 */
	public static String msToTimeStr(long execTime)
	{
		String execTimeHH = "00";
		String execTimeMM = "00";
		String execTimeSS = "00";
		String execTimeMs = "000";

		// MS
		execTimeMs = "000" + execTime % 1000;
		execTimeMs = execTimeMs.substring(execTimeMs.length()-3);
		execTime = execTime / 1000;

		// Seconds
		execTimeSS = "00" + execTime % 60;
		execTimeSS = execTimeSS.substring(execTimeSS.length()-2);
		execTime = execTime / 60;

		// Minutes
		execTimeMM = "00" + execTime % 60;
		execTimeMM = execTimeMM.substring(execTimeMM.length()-2);
		execTime = execTime / 60;

		// Hour
//		execTimeHH = "00" + execTime % 60;
//		execTimeHH = execTimeHH.substring(execTimeHH.length()-2);
//		execTime = execTime / 60;
		execTimeHH = "00" + execTime;
		execTimeHH = (execTime < 100) ? execTimeHH.substring(execTimeHH.length()-2) : "" + execTime;

		return execTimeHH+":"+execTimeMM+":"+execTimeSS+"."+execTimeMs;
	}

	/**
	 * Convert a long into a time string 
	 * 
	 * @param format %HH:%MM:%SS.%ms  or %?HH[:]%MM:%SS.%ms where the HH is optional and only filled in if hour is above 0
	 *        TODO: %D[d ] -->>> optional paramater, which will hold # days (if hours is above 24)
	 * @param execTime
	 * @return a string of the format description
	 */
	public static String msToTimeStrOLD(String format, long execTime)
	{
		if (format == null) throw new RuntimeException("msToTimeStr(): format can't be null");
		if (format.trim().length() == 0) throw new RuntimeException("msToTimeStr(): format can't be empty");

		String execTimeHH = "00";
		String execTimeMM = "00";
		String execTimeSS = "00";
		String execTimeMs = "000";

		// MS
		execTimeMs = "000" + execTime % 1000;
		execTimeMs = execTimeMs.substring(execTimeMs.length()-3);
		execTime = execTime / 1000;

		// Seconds
		execTimeSS = "00" + execTime % 60;
		execTimeSS = execTimeSS.substring(execTimeSS.length()-2);
		execTime = execTime / 60;

		// Minutes
		execTimeMM = "00" + execTime % 60;
		execTimeMM = execTimeMM.substring(execTimeMM.length()-2);
		execTime = execTime / 60;

		// Hour
//		execTimeHH = "00" + execTime % 60;
//		execTimeHH = execTimeHH.substring(execTimeHH.length()-2);
//		execTime = execTime / 60;
		execTimeHH = "00" + execTime;
		execTimeHH = (execTime < 100) ? execTimeHH.substring(execTimeHH.length()-2) : "" + execTime;

		// Handle optional TAGS
		// hour is above or below 00
		String tagStrHH = "%?HH";
		if (format.indexOf(tagStrHH) >= 0)
		{
			String withinSquareBrakets = "";
			int tagStartPos = format.indexOf(tagStrHH+"[");
			if (tagStartPos >= 0)
			{
				int tagEndPos = format.indexOf("]", tagStartPos); // we should possible check for new tags... %MM
				if (tagEndPos >= 0)
				{
					withinSquareBrakets = format.substring(tagStartPos + (tagStrHH+"[").length(), tagEndPos);
				}

				tagStrHH = tagStrHH + "[" + withinSquareBrakets + "]";
			}
			if ( "00".equals(execTimeHH))
				format = format.replace(tagStrHH, ""); // if 00: remove the TAG from the format string
			else
				format = format.replace(tagStrHH, execTimeHH + withinSquareBrakets); 
		}

		format = format.replace("%HH", execTimeHH);
		format = format.replace("%MM", execTimeMM);
		format = format.replace("%SS", execTimeSS);
		format = format.replace("%ms", execTimeMs);

		return format;
	}

	/** seconds as a string in format %?DD[d ]%HH:%MM:%SS */
	public static String secToTimeStrLong(long execTimeInSec)
	{
		return msToTimeStr("%?DD[d ]%HH:%MM:%SS", execTimeInSec * 1000);
	}
	/** seconds as a string in format %?DD[d ]%?HH[:]%MM:%SS */
	public static String secToTimeStrShort(long execTimeInSec)
	{
		return msToTimeStr("%?DD[d ]%?HH[:]%MM:%SS", execTimeInSec * 1000);
	}


	/** ms as a string in format %?DD[d ]%HH:%MM:%SS.%ms */
	public static String msToTimeStrLong(long execTimeInMs)
	{
		return msToTimeStr("%?DD[d ]%HH:%MM:%SS.%ms", execTimeInMs);
	}

	/** ms as a string in format %?DD[d ]%?HH[:]%MM:%SS.%ms */
	public static String msToTimeStrShort(long execTimeInMs)
	{
		return msToTimeStr("%?DD[d ]%?HH[:]%MM:%SS.%ms", execTimeInMs);
	}

	/** microsecond as a string in format %?DD[d ]%HH:%MM:%SS.%ms */
	public static String usToTimeStrLong(long execTimeInUs)
	{
		return msToTimeStr("%?DD[d ]%HH:%MM:%SS.%ms", execTimeInUs/1000);
	}

	/** microsecond as a string in format %?DD[d ]%?HH[:]%MM:%SS.%ms */
	public static String usToTimeStrShort(long execTimeInUs)
	{
		return msToTimeStr("%?DD[d ]%?HH[:]%MM:%SS.%ms", execTimeInUs/1000);
	}

	/**
	 * Convert a long into a time string 
	 * 
	 * @param format %HH:%MM:%SS.%ms  or %?HH[:]%MM:%SS.%ms where the HH is optional and only filled in if hour is above 0
	 *        ?%DD[d ] -->>> optional parameter, which will hold # days (if hours is above 24)
	 * @param execTime
	 * @return a string of the format description
	 */
	public static String msToTimeStr(String format, long execTime)
	{
		if (format == null) throw new RuntimeException("msToTimeStr(): format can't be null");
		if (format.trim().length() == 0) throw new RuntimeException("msToTimeStr(): format can't be empty");

//		String originFormat = format;

		long hoursAbs   = TimeUnit.MILLISECONDS.toHours(execTime);
//		long minutesAbs = TimeUnit.MILLISECONDS.toMinutes(execTime);
//		long secondsAbs = TimeUnit.MILLISECONDS.toSeconds(execTime);

		long days    = TimeUnit.MILLISECONDS.toDays(execTime);
		long hours   = TimeUnit.MILLISECONDS.toHours(execTime)   - TimeUnit.DAYS   .toHours(  TimeUnit.MILLISECONDS.toDays(execTime));
		long minutes = TimeUnit.MILLISECONDS.toMinutes(execTime) - TimeUnit.HOURS  .toMinutes(TimeUnit.MILLISECONDS.toHours(execTime));
		long seconds = TimeUnit.MILLISECONDS.toSeconds(execTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(execTime));
		long millis  = TimeUnit.MILLISECONDS.toMillis(execTime)  - TimeUnit.SECONDS.toMillis( TimeUnit.MILLISECONDS.toSeconds(execTime));

		boolean hasDaysFormat  = format.indexOf("%DD") >= 0 || format.indexOf("%?DD") >= 0;
//		boolean hasHoursFormat = format.indexOf("%HH") >= 0 || format.indexOf("%?HH") >= 0;

//		 System.out.println("----------------------- days="+days+", hours="+hours+" hoursAbs("+hoursAbs+"), minutes="+minutes+", seconds="+seconds+", ms="+millis+".");
		
		// if we DO NOT have "%DD", then we want hours to be presented as long 30 for 30 hours
		if ( ! hasDaysFormat )
			hours = hoursAbs;

		String execTimeDD = String.format("%d",   days);
		String execTimeHH = String.format("%02d", hours);
		String execTimeMM = String.format("%02d", minutes);
		String execTimeSS = String.format("%02d", seconds);
		String execTimeMs = String.format("%03d", millis);

//		System.out.println("----------------------- DD="+execTimeDD+", HH="+execTimeHH+" MM("+execTimeMM+"), SS="+execTimeSS+", Ms="+execTimeMs+".");

		// Handle optional TAGS
		// hour is above or below 00
		String tagStrHH = "%?HH";
		if (format.indexOf(tagStrHH) >= 0)
		{
			String withinSquareBrakets = "";
			int tagStartPos = format.indexOf(tagStrHH+"[");
			if (tagStartPos >= 0)
			{
				int tagEndPos = format.indexOf("]", tagStartPos); // we should possible check for new tags... %MM
				if (tagEndPos >= 0)
				{
					withinSquareBrakets = format.substring(tagStartPos + (tagStrHH+"[").length(), tagEndPos);
				}

				tagStrHH = tagStrHH + "[" + withinSquareBrakets + "]";
			}
			if (hoursAbs == 0)
				format = format.replace(tagStrHH, ""); // if 00: remove the TAG from the format string
			else
				format = format.replace(tagStrHH, execTimeHH + withinSquareBrakets); 
		}

		// Handle optional TAGS
		// Day is above or below 00
		String tagStrDay = "%?DD";
		if (format.indexOf(tagStrDay) >= 0)
		{
			String withinSquareBrakets = "";
			int tagStartPos = format.indexOf(tagStrDay+"[");
			if (tagStartPos >= 0)
			{
				int tagEndPos = format.indexOf("]", tagStartPos); // we should possible check for new tags... %MM
				if (tagEndPos >= 0)
				{
					withinSquareBrakets = format.substring(tagStartPos + (tagStrDay+"[").length(), tagEndPos);
				}

				tagStrDay = tagStrDay + "[" + withinSquareBrakets + "]";
			}
			if (days == 0)
				format = format.replace(tagStrDay, ""); // if 00: remove the TAG from the format string
			else
				format = format.replace(tagStrDay, execTimeDD + withinSquareBrakets); 
		}

		format = format.replace("%DD", execTimeDD);
		format = format.replace("%HH", execTimeHH);
		format = format.replace("%MM", execTimeMM);
		format = format.replace("%SS", execTimeSS);
		format = format.replace("%ms", execTimeMs);

		return format;
	}

	public static String msToTimeStrDHMSms(long duration)
	{
		String res = ""; // java.util.concurrent.TimeUnit;
		
		long days    = TimeUnit.MILLISECONDS.toDays(duration);
		long hours   = TimeUnit.MILLISECONDS.toHours(duration)   - TimeUnit.DAYS   .toHours(  TimeUnit.MILLISECONDS.toDays(duration));
		long minutes = TimeUnit.MILLISECONDS.toMinutes(duration) - TimeUnit.HOURS  .toMinutes(TimeUnit.MILLISECONDS.toHours(duration));
		long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration));
		long millis  = TimeUnit.MILLISECONDS.toMillis(duration)  - TimeUnit.SECONDS.toMillis( TimeUnit.MILLISECONDS.toSeconds(duration));

		if ( days == 0 )
			res = String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis);
		else
			res = String.format("%dd %02d:%02d:%02d.%03d", days, hours, minutes, seconds, millis);

		return res;
	}

	public static String msToTimeStrDHMS(long duration)
	{
		String res = ""; // java.util.concurrent.TimeUnit;
		
		long days    = TimeUnit.MILLISECONDS.toDays(duration);
		long hours   = TimeUnit.MILLISECONDS.toHours(duration)   - TimeUnit.DAYS   .toHours(  TimeUnit.MILLISECONDS.toDays(duration));
		long minutes = TimeUnit.MILLISECONDS.toMinutes(duration) - TimeUnit.HOURS  .toMinutes(TimeUnit.MILLISECONDS.toHours(duration));
		long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration));
//		long millis  = TimeUnit.MILLISECONDS.toMillis(duration)  - TimeUnit.SECONDS.toMillis( TimeUnit.MILLISECONDS.toSeconds(duration));

		if ( days == 0 )
			res = String.format("%02d:%02d:%02d", hours, minutes, seconds);
		else
			res = String.format("%dd %02d:%02d:%02d", days, hours, minutes, seconds);

		return res;
	}

	public static String usToTimeStrDHMS(long duration)
	{
		String res = ""; // java.util.concurrent.TimeUnit;
		
		long days    = TimeUnit.MICROSECONDS.toDays(duration);
		long hours   = TimeUnit.MICROSECONDS.toHours(duration)   - TimeUnit.DAYS   .toHours(  TimeUnit.MICROSECONDS.toDays(duration));
		long minutes = TimeUnit.MICROSECONDS.toMinutes(duration) - TimeUnit.HOURS  .toMinutes(TimeUnit.MICROSECONDS.toHours(duration));
		long seconds = TimeUnit.MICROSECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MICROSECONDS.toMinutes(duration));
//		long millis  = TimeUnit.MICROSECONDS.toMillis(duration)  - TimeUnit.SECONDS.toMillis( TimeUnit.MICROSECONDS.toSeconds(duration));

		if ( days == 0 )
			res = String.format("%02d:%02d:%02d", hours, minutes, seconds);
		else
			res = String.format("%dd %02d:%02d:%02d", days, hours, minutes, seconds);

		return res;
	}

	public static final int DD_MS          = 0;
	public static final int DD_MILLISECOND = DD_MS;
	public static final int DD_SS          = 1;
	public static final int DD_SECOND      = DD_SS;
	public static final int DD_MI          = 2;
	public static final int DD_MINUTE      = DD_MI;
	public static final int DD_HH          = 3;
	public static final int DD_HOUR        = DD_HH;
	public static final int DD_DD          = 4;
	public static final int DD_DAY         = DD_DD;
	public static final int DD_WK          = 5;
	public static final int DD_WEEK        = DD_WK;
	public static final int DD_MM          = 6;
	public static final int DD_MONTH       = DD_MM;

	public static int datediff(int type, Date startDate, Date endDate)
	{
		return datediff( type, startDate.getTime(), endDate.getTime() );
	}
	public static int datediff(int type, long startDate, long endDate)
	{
		long diffInSec = ((endDate / 1000) - (startDate / 1000));
		long ret = 0;

		switch (type)
		{
			case DD_MILLISECOND:
				ret = endDate - startDate;
				break;

			case DD_SECOND:
				ret = diffInSec;
				break;

			case DD_MINUTE:
				ret = diffInSec / 60;
				break;

			case DD_HOUR:
				ret = diffInSec / 60 / 60;
				break;

			case DD_DAY:
				ret = diffInSec / 60 / 60 / 24;
				break;

			case DD_WEEK:
				ret = diffInSec  / 60 / 60 / 24 / 7;
				break;

			case DD_MONTH:
				// Comparing ASE datediff(mm) on month and the below method
				// showed that using 365 days per year seemed to work all
				// the way upp to year 2069. Then this system will hopefully
				//  NOT be around anymore. (and I will be 100 years old :^)
				ret = diffInSec  / 2628000;   // 2628000 = 365      / 12 * 24 * 60 * 60;
				//ret = diffInSec  / 2629800; // 2629800 = 365.25   / 12 * 24 * 60 * 60;
				//ret = diffInSec  / 2629746; // 2629746 = 365.2425 / 12 * 24 * 60 * 60;
				break;

		}

		return (int) ret;
	}

	/**
	 * Parse the string into a Tiemstamp<br>
	 * The format is tried in the following order 
	 * <ul>
	 *     <li> <code>yyyy-MM-dd'T'HH:mm:ss.SSSXXX</code> - ISO 8601</li>
	 *     <li> <code>yyyy-MM-dd' 'HH:mm:ss.SSS</code> </li>
	 *     <li> <code>yyyy-MM-dd' 'HH:mm:ss</code> </li>
	 *     <li> <code>yyyy-MM-dd' 'HH:mm</code> </li>
	 *     <li> <code>yyyy-MM-dd' 'HH</code> </li>
	 *     <li> <code>yyyy-MM-dd</code> </li>
	 * </ul>
	 * 
	 * @param str      The time String
	 * @return         A Timestamp
	 * @throws ParseException    if the string cannot be parsed.
	 */
	public static Timestamp parseToTimestampX(String str) 
	throws ParseException
	{
		List<String> knownPatterns = new ArrayList<>();
		knownPatterns.add("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"); // ISO 8601
		knownPatterns.add("yyyy-MM-dd' 'HH:mm:ss.SSS");
		knownPatterns.add("yyyy-MM-dd' 'HH:mm:ss");
		knownPatterns.add("yyyy-MM-dd' 'HH:mm");
		knownPatterns.add("yyyy-MM-dd' 'HH");
		knownPatterns.add("yyyy-MM-dd");

		for (String pattern : knownPatterns) 
		{
		    try 
		    {
		        // Take a try
		    	SimpleDateFormat sdf = new SimpleDateFormat(pattern);
				Date date = sdf.parse(str);
				return new Timestamp(date.getTime());
		    } 
		    catch (ParseException pe) 
		    {
		        // Loop on
		    }
		}
		throw new ParseException("No known Date format found for '" + str + "', tested following patterns "+knownPatterns+".", 0);
	}

	/**
	 * Parse the string into a Tiemstamp<br>
	 * The format is <code>yyyy-MM-dd HH:mm:ss.SSS</code><br>
	 * If we get a ParseException, we will return the paremeter 'defaultVal'
	 * 
	 * @param str          The time String
	 * @param defaultVal   If we get a ParseException, we will return this value instead
	 * @return             A Timestamp
	 */
	public static Timestamp parseToTimestampNoThrow(String str, Timestamp defaultVal) 
	{
		try{ return parseToTimestamp(str, "yyyy-MM-dd HH:mm:ss.SSS"); }
		catch(ParseException ex) { return defaultVal; }
	}

	/**
	 * Parse the string into a Tiemstamp<br>
	 * The format is <code>yyyy-MM-dd HH:mm:ss.SSS</code>
	 * 
	 * @param str      The time String
	 * @return         A Timestamp
	 * @throws ParseException    if the string cannot be parsed.
	 */
	public static Timestamp parseToTimestamp(String str) 
	throws ParseException
	{
		return parseToTimestamp(str, "yyyy-MM-dd HH:mm:ss.SSS");
	}

	/**
	 * Parse the string into a Timestamp<br>
	 * 
	 * @param str      The time String
	 * @param format   The format you want to use (according to: https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html)
	 * @return         A Timestamp
	 * @throws ParseException    if the string cannot be parsed.
	 */
	public static Timestamp parseToTimestamp(String str, String format) 
	throws ParseException
	{
		SimpleDateFormat dateFormat = new SimpleDateFormat(format);
		Date date = dateFormat.parse(str);
		Timestamp ts = new Timestamp(date.getTime());
		return ts;
	}

	/**
	 * Parse the string into a UTC Timestamp<br>
	 * 
	 * @param str      The time String
	 * @param format   The format you want to use (according to: https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html)
	 * @return         A Timestamp (note: a toString() will still print it as Local Time depending on what TimeZone you are at)
	 * @throws ParseException    if the string cannot be parsed.
	 */
	public static Timestamp parseToUtcTimestamp(String str, String format) 
	throws ParseException
	{
		SimpleDateFormat dateFormat = new SimpleDateFormat(format);
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

		Date date = dateFormat.parse(str);
		Timestamp ts = new Timestamp(date.getTime());
		return ts;
	}

	/**
	 * Parse the string into a Tiemstamp<br>
	 * The format is <code>yyyy-MM-dd HH:mm:ss.SSS</code>
	 * 
	 * @param str      The time String
	 * @return         A Timestamp
	 * @throws ParseException    if the string cannot be parsed.
	 */
	public static Timestamp parseToTimestampIso8601(String str) 
	throws ParseException
	{
		synchronized (ISO8601_DATE_FORMAT)
		{
			Date date = ISO8601_DATE_FORMAT.parse(str);
			Timestamp ts = new Timestamp(date.getTime());
			return ts;
		}
	}

	/**
	 * Format a Timestamp to a ISO 8601 String<br>
	 * format is: "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"<br>
	 * Example output: "2018-01-08T09:56:53.716+01:00"
	 * @param ts
	 * @return String in ISO 8601 format
	 */
	public static String toStringIso8601(Timestamp ts)
	{
		synchronized (ISO8601_DATE_FORMAT)
		{
			return ISO8601_DATE_FORMAT.format(ts);
		}
	}

	/**
	 * Format a Timestamp to a ISO 8601 String<br>
	 * format is: "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"<br>
	 * Example output: "2018-01-08T09:56:53.716+01:00"
	 * @param ts
	 * @return String in ISO 8601 format
	 */
	public static String toStringIso8601(long ts)
	{
		synchronized (ISO8601_DATE_FORMAT)
		{
			return ISO8601_DATE_FORMAT.format( new Date(ts) );
		}
	}

	/**
	 * Format a Timestamp to a String<br>
	 * format is: "yyyy-MM-dd HH:mm:ss.SSS"<br>
	 * Example output: "2018-01-08 09:56:53.716"
	 * @param ts
	 * @return String in above format
	 */
	public static String toString(Timestamp ts)
	{
		synchronized (DATE_FORMAT)
		{
			return DATE_FORMAT.format(ts);
		}
	}

	/**
	 * Format a Timestamp to a String<br>
	 * format is: "yyyy-MM-dd HH:mm:ss.SSS"<br>
	 * Example output: "2018-01-08 09:56:53.716"
	 * @param ts
	 * @return String in above format
	 */
	public static String toString(Date ts)
	{
		synchronized (DATE_FORMAT)
		{
			return DATE_FORMAT.format(ts);
		}
	}

	/**
	 * Format a Timestamp to a Local TimeZone String<br>
	 * format is: "yyyy-MM-dd HH:mm:ss.SSS"<br>
	 * Example output: "2018-01-08 09:56:53.716"
	 * @param ts
	 * @return String in above format
	 */
	public static String toString(long ts)
	{
		synchronized (DATE_FORMAT)
		{
			return DATE_FORMAT.format( new Date(ts) );
		}
	}


	/**
	 * Format a Timestamp to a UTC String<br>
	 * format is: "yyyy-MM-dd HH:mm:ss.SSS"<br>
	 * Example output: "2018-01-08 09:56:53.716"
	 * @param ts
	 * @return String in above format
	 */
	public static String toStringUtc(long ts)
	{
		synchronized (DATE_FORMAT)
		{
			return UTC_DATE_FORMAT.format( new Date(ts));
		}
	}


	//--------------------------------------------------------------------------------
	/**
	 * in format 'yyyy-MM-dd_HHmmss'
	 * 
	 * @param time  time in ms like System.currentTimeMillis()
	 * @return string 'yyyy-MM-dd_HHmmss'
	 */
	public static String getCurrentTimeForFileNameYmdHms(long ts)
	{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
		return sdf.format( new Date(ts) );
	}

	/**
	 * in format 'yyyy-MM-dd_HHmmss'
	 * 
	 * @return string 'yyyy-MM-dd_HHmmss'
	 */
	public static String getCurrentTimeForFileNameYmdHms()
	{
		return getCurrentTimeForFileNameYmdHms( System.currentTimeMillis() );
	}
	

	
	
	//--------------------------------------------------------------------------------
	/**
	 * in format 'yyyy-MM-dd_HHmm'
	 * 
	 * @param time  time in ms like System.currentTimeMillis()
	 * @return string 'yyyy-MM-dd_HHmm'
	 */
	public static String getCurrentTimeForFileNameYmdHm(long ts)
	{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmm");
		return sdf.format( new Date(ts) );
	}

	/**
	 * in format 'yyyy-MM-dd_HHmm'
	 * 
	 * @return string 'yyyy-MM-dd_HHmm'
	 */
	public static String getCurrentTimeForFileNameYmdHm()
	{
		return getCurrentTimeForFileNameYmdHm( System.currentTimeMillis() );
	}

	
	
	//--------------------------------------------------------------------------------
	/**
	 * in format 'yyyy-MM-dd'
	 * 
	 * @param time  time in ms like System.currentTimeMillis()
	 * @return string 'yyyy-MM-dd'
	 */
	public static String getCurrentTimeForFileNameYmd(long ts)
	{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		return sdf.format( new Date(ts) );
	}

	/**
	 * in format 'yyyy-MM-dd'
	 * 
	 * @return string 'yyyy-MM-dd'
	 */
	public static String getCurrentTimeForFileNameYmd()
	{
		return getCurrentTimeForFileNameYmdHms( System.currentTimeMillis() );
	}

	
	
	//--------------------------------------------------------------------------------
	/**
	 * in format 'HHmmss'
	 * 
	 * @param time  time in ms like System.currentTimeMillis()
	 * @return string 'HHmmss'
	 */
	public static String getCurrentTimeForFileNameHms(long ts)
	{
		SimpleDateFormat sdf = new SimpleDateFormat("HHmmss");
		return sdf.format( new Date(ts) );
	}

	/**
	 * in format 'HHmmss'
	 * 
	 * @return string 'HHmmss'
	 */
	public static String getCurrentTimeForFileNameHms()
	{
		return getCurrentTimeForFileNameYmdHms( System.currentTimeMillis() );
	}

	
	
	//--------------------------------------------------------------------------------
	/**
	 * in format 'HHmm'
	 * 
	 * @param time  time in ms like System.currentTimeMillis()
	 * @return string 'HHmm'
	 */
	public static String getCurrentTimeForFileNameHm(long ts)
	{
		SimpleDateFormat sdf = new SimpleDateFormat("HHmm");
		return sdf.format( new Date(ts) );
	}

	/**
	 * in format 'HHmm'
	 * 
	 * @return string 'HHmm'
	 */
	public static String getCurrentTimeForFileNameHm()
	{
		return getCurrentTimeForFileNameYmdHm( System.currentTimeMillis() );
	}
	

	
	
	
	//--------------------------------------------------------------------------------
	/**
	 * Format the input as "old C style" using strftime formating.
	 * 
	 * @param inputStr    The string with strftime parameters
	 * @return
	 */
	public static String strftime(String string)
	{
		Strftime strftime = new Strftime(string);
		return strftime.format(new Date());
	}

	/*
	 * *********************************************************************
	 * ******* SUB CLASSES ******** SUB CLASSES ******** SUB CLASSES *******
	 * *********************************************************************
	 */


	/*
	 * *********************************************************************
	 * ********* TEST CODE ********* TEST CODE ********* TEST CODE *********
	 * *********************************************************************
	 */
	private static void test(String format, String expected, long ms)
	{
		String result = TimeUtils.msToTimeStr(format, ms);

		if (expected.equals(result))
			System.out.println("  OK: result '"+result+"' equals expected value '"+expected+"'.");
		else
			System.out.println("FAIL: result '"+result+"' IS NOT equal the expected value '"+expected+"'.");
	}
	public static void main(String[] args)
	{
		test("%MM:%SS.%ms",          "00:10.000",      10 * 1000);
		test("%MM:%SS.%ms",          "01:01.000",      61 * 1000);
		test("%?HH[:]%MM:%SS.%ms",   "01:01.000",      61 * 1000);
		test("%?HH[:]%MM:%SS.%ms",   "01:01:01.000",   (3600 + 61) * 1000);
		test("%?HH[---]%MM:%SS.%ms", "01:01.000",      61 * 1000);
		test("%?HH[---]%MM:%SS.%ms", "01---01:01.000", (3600 + 61) * 1000);
		test("%?HH[---]%MM:%SS.%ms", "24---01:01.000", (3600*24 + 61) * 1000);
		test("%?DD[d ]%?HH[---]%MM:%SS.%ms", "1d 04---01:01.000", (3600*28 + 61) * 1000);
		test("%?DD[d ]%?HH[---]%MM:%SS.%ms", "7d 23---01:02.000", ((3600*24*7) + (3600*23) + 62) * 1000);
		test("%?DD[ days, ]%?HH[---]%MM:%SS.%ms", "23 days, 23---01:02.000", ((3600*24*23) + (3600*23) + 62) * 1000);
		test("%?DD[ days, ]%?HH[---]%MM:%SS.%ms", "999 days, 23---01:02.000", ((3600*24*999L) + (3600*23) + 62) * 1000);
		test("%?DD[ days, ]%?HH[---]%MM:%SS.%ms", "01:02.000", 62 * 1000);
		test("%?DD[ days, ]%?HH[---]%HH:%MM", "00:01", 62 * 1000);
		test("%?DD[d ]%HH:%MM", "1d 01:02", ((3600*24*1) + (3600*1) + 122) * 1000);
		test("%?DD[d ]%HH:%MM", "01:02", ((3600*1) + 122) * 1000);

//		System.out.println("msToTimeStrDHMSms: "+msToTimeStrDHMSms(10 * 1000));  // 10 sec
//		System.out.println("msToTimeStrDHMSms: "+msToTimeStrDHMSms((3600 + 61) * 1000)); // 1 Hours, 61 minutes
//		System.out.println("msToTimeStrDHMSms: "+msToTimeStrDHMSms((3600 + 61) * 1000)); // 1 Hours, 1 minute, 1 second
//		System.out.println("msToTimeStrDHMSms: "+msToTimeStrDHMSms(((3600*24*1) + (3600*4) + 61) * 1000)); // 1 Hours, 1 minute, 1 second
//		System.out.println("msToTimeStrDHMSms: "+msToTimeStrDHMSms(((3600*24*7) + (3600*23) + 62) * 1000)); // 1 Hours, 1 minute, 1 second
//		System.out.println("msToTimeStrDHMSms: "+msToTimeStrDHMSms(((3600*24*128L) + (3600*23) + 62) * 1000)); // 1 Hours, 1 minute, 1 second
		
//		try
//		{
//			Timestamp ts1 = new Timestamp(System.currentTimeMillis());
//			String str = toStringIso8601(ts1);
//			Timestamp ts2 = parseToTimestampIso8601(str);
//			System.out.println("OK: ts1=|"+ts1+"|, str=|"+str+"|, ts2=|"+ts2+"|.");
//		}
//		catch (Exception e) 
//		{
//			e.printStackTrace();
//		}
//
//		
//		try
//		{
//			String str = "2018-01-08T11:33:19.543+01:00";
//			Timestamp ts2 = parseToTimestampIso8601(str);
//			System.out.println("OK: str=|"+str+"|, ts2=|"+ts2+"|.");
//		}
//		catch (Exception e) 
//		{
//			e.printStackTrace();
//		}
	}
}
