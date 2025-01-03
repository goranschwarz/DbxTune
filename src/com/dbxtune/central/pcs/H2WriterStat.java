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
package com.dbxtune.central.pcs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.Version;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.AlarmEventOsLoadAverage;
import com.dbxtune.alarm.events.AlarmEventOsLoadAverageAdjusted;
import com.dbxtune.central.DbxTuneCentral;
import com.dbxtune.pcs.PersistReader;
import com.dbxtune.pcs.PersistentCounterHandler;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.ConnectionProvider;
import com.dbxtune.utils.CronUtils;
import com.dbxtune.utils.H2UrlHelper;
import com.dbxtune.utils.MovingAverageChart;
import com.dbxtune.utils.MovingAverageCounterManager;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;

import it.sauronsoftware.cron4j.InvalidPatternException;
import it.sauronsoftware.cron4j.SchedulingPattern;

/**
 * Small dummy implementation to keep track of H2 counter statistics
 * <ul>
 *   <li>info.FILE_READ</li>
 *   <li>info.FILE_WRITE</li>
 *   <li>info.PAGE_COUNT</li>
 *   <li>and possibly more</li>
 * </ul>
 *  
 * I also incorporated OsLoadAverage in this... But it will most possible me moved to it's own class later...<br>
 * Note: Right now there are both 'normal' and 'adjusted' load average (one of them will probably be removed)
 * 
 * @author gorans
 *
 */
public class H2WriterStat
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	// implements singleton pattern
	private static H2WriterStat _instance = null;

	public static final String      PROPKEY_sampleOsLoadAverage   = "H2WriterStat.sample.osLoadAverage";
	public static final boolean     DEFAULT_sampleOsLoadAverage   = true;

	public static final String      PROPKEY_AlarmOsLoadAverage1m  = "H2WriterStat.alarm.osLoadAverage.1m.gt";
	public static final Double      DEFAULT_AlarmOsLoadAverage1m  = 99.0; // more or less DISABLED

	public static final String      PROPKEY_AlarmOsLoadAverage5m  = "H2WriterStat.alarm.osLoadAverage.5m.gt";
	public static final Double      DEFAULT_AlarmOsLoadAverage5m  = 99.0; // more or less DISABLED

	public static final String      PROPKEY_AlarmOsLoadAverage15m = "H2WriterStat.alarm.osLoadAverage.15m.gt";
	public static final Double      DEFAULT_AlarmOsLoadAverage15m = 99.0; // more or less DISABLED

	public static final String      PROPKEY_AlarmOsLoadAverage30m = "H2WriterStat.alarm.osLoadAverage.30m.gt";
	public static final Double      DEFAULT_AlarmOsLoadAverage30m = 99.0; // more or less DISABLED

	public static final String      PROPKEY_AlarmOsLoadAverage60m = "H2WriterStat.alarm.osLoadAverage.60m.gt";
	public static final Double      DEFAULT_AlarmOsLoadAverage60m = 99.0; // more or less DISABLED

	// The below is alarms for ADJUSTED values meaning: Adjusted = LoadAverage / numOfProcs
	public static final String      PROPKEY_AlarmOsLoadAverageAdjusted1m  = "H2WriterStat.alarm.osLoadAverage.adjusted.1m.gt";
	public static final Double      DEFAULT_AlarmOsLoadAverageAdjusted1m  = 99.0; // more or less DISABLED

	public static final String      PROPKEY_AlarmOsLoadAverageAdjusted5m  = "H2WriterStat.alarm.osLoadAverage.adjusted.5m.gt";
	public static final Double      DEFAULT_AlarmOsLoadAverageAdjusted5m  = 99.0; // more or less DISABLED

	public static final String      PROPKEY_AlarmOsLoadAverageAdjusted15m = "H2WriterStat.alarm.osLoadAverage.adjusted.15m.gt";
	public static final Double      DEFAULT_AlarmOsLoadAverageAdjusted15m = 4.0;

	public static final String      PROPKEY_AlarmOsLoadAverageAdjusted30m = "H2WriterStat.alarm.osLoadAverage.adjusted.30m.gt";
	public static final Double      DEFAULT_AlarmOsLoadAverageAdjusted30m = 3.0;

	public static final String      PROPKEY_AlarmOsLoadAverageAdjusted60m = "H2WriterStat.alarm.osLoadAverage.adjusted.60m.gt";
	public static final Double      DEFAULT_AlarmOsLoadAverageAdjusted60m = 2.5;

	private static final String     DEFAULT_sampleTimeFormat = "%?DD[d ]%?HH[:]%MM:%SS.%ms";

	private String                  _sampleTimeFormat = DEFAULT_sampleTimeFormat;
	private long                    _lastRefresh = -1;
	private long                    _lastIntervallInMs = -1;
	private Map<String, Long>       _lastAbs  = null;
	private Map<String, Long>       _lastDiff = new HashMap<>();
	private Map<String, BigDecimal> _lastRate = new HashMap<>();
	
	private ConnectionProvider _connProvider = null;
	private File _h2DbFile = null;

	// If we should get OS load average or not
	private boolean _get_osLoadAverage  = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_sampleOsLoadAverage, DEFAULT_sampleOsLoadAverage);
	private double  _osLoadAverage1min  = -1;
	private double  _osLoadAverage5min  = -1;
	private double  _osLoadAverage15min = -1;
	private double  _osLoadAverage30min = -1;
	private double  _osLoadAverage60min = -1;
	private double  _osLoadAverageAdjusted1min  = -1;
	private double  _osLoadAverageAdjusted5min  = -1;
	private double  _osLoadAverageAdjusted15min = -1;
	private double  _osLoadAverageAdjusted30min = -1;
	private double  _osLoadAverageAdjusted60min = -1;
	private LinkedList<OsLoadAvgEntry> _osLoadAverageList = null;

	public static final String FILE_READ  = "info.FILE_READ";
	public static final String FILE_WRITE = "info.FILE_WRITE";
	public static final String PAGE_COUNT = "info.PAGE_COUNT";

//	public static final String H2_FILE_NAME    = "dbxtune.H2.FILE_NAME";
	public static final String H2_FILE_SIZE_KB = "dbxtune.H2.FILE_SIZE_KB";
	public static final String H2_FILE_SIZE_MB = "dbxtune.H2.FILE_SIZE_MB";


	//////////////////////////////////////////////
	//// Instance
	//////////////////////////////////////////////

	/** 
	 * get current instance, if no instance has been assigned a new dummy will be created, with NO ConnectionProvider
	 * <p>
	 * Note: to check if a ConnectionProvider has been installed, use: H2WriterStat.getInstanceConnectionProvider()
	 * 
	 * @return do NOT return null (a dummy instance will be created if none has been installed)
	 */
	public synchronized static H2WriterStat getInstance()
	{
		if (_instance == null)
			_instance = new H2WriterStat(null);

		return _instance;
	}

	public static boolean hasInstance()
	{
		return (_instance != null);
	}

	public static ConnectionProvider getInstanceConnectionProvider()
	{
		if (_instance == null)
			return null;

		return _instance._connProvider;
	}

	public static void setInstance(H2WriterStat inst)
	{
		_instance = inst;
	}

	public H2WriterStat(ConnectionProvider connProvider)
	{
		_connProvider = connProvider;
		
		printAlarmConfig();
	}

	/**
	 * Set the time format used when printing the sample time
	 * @param fmt Format, set to null or "" will set to default. (example: <code>%?DD[d ]%?HH[:]%MM:%SS.%ms</code>)
	 */
	public void setSampleTimeFormat(String fmt)
	{
		_sampleTimeFormat = fmt;

		if (StringUtil.isNullOrBlank(_sampleTimeFormat))
			_sampleTimeFormat = DEFAULT_sampleTimeFormat;
	}

	/**
	 * Get the time format used when printing the sample time.
	 * <p>
	 * default: <code>%?DD[d ]%?HH[:]%MM:%SS.%ms</code>
	 * 
	 * @return always a string
	 */
	public String getSampleTimeFormat()
	{
		if (StringUtil.isNullOrBlank(_sampleTimeFormat))
			_sampleTimeFormat = DEFAULT_sampleTimeFormat;
		
		return _sampleTimeFormat;
	}
	
	/** 
	 * Grab the connection to H2 from "somewhere", possibly the PcsReader 
	 * <p>
	 * FIXME: implement some kind of ConnectionProvider, that is installed by the creator 
	 *        This enables us to use this for other writers than the "central" one.
	 */
	public H2WriterStat refreshCounters()
	{
		if (_connProvider == null)
		{
			_logger.debug("No Connection Provider has been installed. Skipping refresh of H2-Performance-Counters.");
			return this;
		}

		// Grab a connection, if one cound NOT be provided, return
		DbxConnection conn = _connProvider.getConnection();
		if (conn == null)
		{
			_logger.info("No Connection could be provided be the ConnectionProvider. Skipping refresh of H2-Performance-Counters.");
			return this;
		}
		
		// Get the current used URL, then get the H2 file from the URL.
		try
		{
			String url = conn.getMetaData().getURL();

			if ( url != null && url.startsWith("jdbc:h2:") )
			{
				H2UrlHelper urlHelper = new H2UrlHelper(url);
				_h2DbFile = urlHelper.getDbFile();
				if (_h2DbFile == null)
				{
					_logger.warn("Can't extract H2 database file from the URL '"+url+"'. Skipping checking the File Size.");
				}
				
			}
			
		}
		catch (SQLException ex)
		{
			_h2DbFile = null;
			_logger.warn("Skipping examin the H2-File-Size. Got problems when getting the URL from the connections metadata. Skipping checking the File Size. Caught: "+ex);
		}

		try
		{
			refreshCounters(conn);
		}
		finally 
		{
			_connProvider.releaseConnection(conn);
		}

		return this;
	}
	
	public H2WriterStat refreshCounters(Connection conn)
	{
//		String sql = "select #NAME#, cast(#VALUE# as bigint) as #VALUE# from #INFORMATION_SCHEMA#.#SETTINGS# where #NAME# in ('"+FILE_READ+"', '"+FILE_WRITE+"', '"+PAGE_COUNT+"')";
		String sql = "select #SETTING_NAME#, cast(#SETTING_VALUE# as bigint) as #SETTING_VALUE# from #INFORMATION_SCHEMA#.#SETTINGS# where #SETTING_NAME# in ('"+FILE_READ+"', '"+FILE_WRITE+"', '"+PAGE_COUNT+"')";
		sql = sql.replace('#',  '"'); // Replace all # into H2 QuotedIdentifier

		try (Statement stmnt = conn.createStatement())
		{
			stmnt.setQueryTimeout(1);
			ResultSet rs = stmnt.executeQuery(sql);
					
			Map<String, Long> map = new HashMap<>();
			while (rs.next())
			{
				String key = rs.getString(1);
				Long   val = rs.getLong  (2);

				// do DIFF calculation and store
				map.put(key, val);
			}
			rs.close();

			// Get the file size, and stuff it in the MAP
			if (_h2DbFile != null)
			{
				if (_h2DbFile.exists())
				{
					long   h2FileSizeKb = _h2DbFile.length() / 1024;
					long   h2FileSizeMb = _h2DbFile.length() / 1024 / 1024;
					//String h2FileName = FilenameUtils.getName(_h2DbFile.getAbsolutePath());

					map.put(H2_FILE_SIZE_KB, h2FileSizeKb);
					map.put(H2_FILE_SIZE_MB, h2FileSizeMb);
				}
			}

			// Skip logic on FIRST refresh
			if (_lastAbs != null)
			{
				_lastIntervallInMs = System.currentTimeMillis() - _lastRefresh;
				
				if (_lastIntervallInMs > 0)
				{
					for (String key : map.keySet())
					{
						Long prevVal = _lastAbs.get(key);
						Long thisVal = map.get(key);

						// Calculate the RATE
						Long diffVal = thisVal - prevVal;
						BigDecimal rate = null;
						try 
						{
							rate = new BigDecimal( ((diffVal*1.0) / (_lastIntervallInMs*1.0)) * 1000 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
						} 
						catch (NumberFormatException ex) 
						{
							_logger.warn("Calculating RATE value had problems. diffVal="+diffVal+" (divided by) _lastIntervallInMs="+_lastIntervallInMs+". Setting rate to 0.0  Caught: " + ex);
							rate = new BigDecimal(0.0);
						}

						// stuff it in the DIFF and RATE MAP
						_lastDiff.put(key, diffVal);
						_lastRate.put(key, rate);
					}
				}
			}
			_lastAbs     = map;
			_lastRefresh = System.currentTimeMillis();
		}
		catch(SQLException ex)
		{
			_logger.warn("Problems refreshing H2 Performance Counters. Caught: " + ex);
		}

		// Get "Load Average" from the OS (only 1 minute is available from the MxBean)
		if (_get_osLoadAverage)
		{
			try
			{
				// sometimes we get NullPointerExceptions when we iterate the list in below method...
				// if that's the case... simply create a new list and "hope for the best"
				calcLoadAverage();

				checkForAlarms();
			}
			catch (RuntimeException ex)
			{
				_logger.error("Problems in H2WriterStat.calcLoadAverage(). Creating a new LinkedList for '_osLoadAverageList'.", ex);
				_osLoadAverageList = new LinkedList<>();
			}
		}

		_lastRefresh = System.currentTimeMillis();
		return this;
	}

	private synchronized void calcLoadAverage()
	{
		// Get ONE minute Load Average From OS (if the OS isn't supporting this, -1 will be returned)
		OperatingSystemMXBean os  = ManagementFactory.getOperatingSystemMXBean();
		double osLoadAverage1min  = os.getSystemLoadAverage();
		double numOfProcs         = os.getAvailableProcessors() * 1.0; // * 1.0 to convert it to a double
		
		// if NOT SUPPORTED, do nothing...
		if (osLoadAverage1min == -1)
			return;

		long now = System.currentTimeMillis();
		
		if (_osLoadAverageList == null)
			_osLoadAverageList = new LinkedList<>();

		// Calculate 5, 15, 30 and 60 minutes into milliseconds
		long   ms5m   = 300_000; // 5 * 60 * 1000;
		int    cnt5m  = 0;
		double sum5m  = 0;

		long   ms15m  = 900_000; // 15 * 60 * 1000;
		int    cnt15m = 0;
		double sum15m = 0;

		long   ms30m  = 1_800_000; // 30 * 60 * 1000;
		int    cnt30m = 0;
		double sum30m = 0;

		long   ms60m  = 3_600_000; // 60 * 60 * 1000;
		int    cnt60m = 0;
		double sum60m = 0;

		// discard entries from the LoadAverage LIST after this number of milliseconds 
		long maxAge = ms60m;
		
		
		// Add current entry to the list
		_osLoadAverageList.add(new OsLoadAvgEntry(now, osLoadAverage1min));

		//
		// Get oldest entry so we can check if we have enough time/samples to calculate 5, 15, 30 and 60 minute values 
		//
		// 60, 30, 15 and 5 minute would typically be HIGH at start time (since it's the same value as 1 minute)... 
		// So we might want to "skip" those until we have enough data point to measure (or similar)
		// Otherwise we will have *ALARMS* that are "faulty".
		OsLoadAvgEntry oldestEntry = _osLoadAverageList.getFirst(); // note: new entries are added at "END", so oldest entry would be FIRST
		long oldestAgeInMs = -1;
		if (oldestEntry != null)
			oldestAgeInMs = now - oldestEntry._ts;

		// Loop the list and remove old entries
		// and calculate the Average for 5, 15, 30 and 60 minutes
		for(Iterator<OsLoadAvgEntry> i = _osLoadAverageList.iterator(); i.hasNext();) 
		{
			OsLoadAvgEntry entry = i.next();
			long   ts  = entry._ts;
			double val = entry._val;

			long entryAgeMs = now - ts;
			
			// Remove entries that are older than 60 minutes (or the MAX interval to save)
			// and get next entry (start at the top)
			if ( entryAgeMs > maxAge )
			{
				i.remove();
				continue;
			}
			
			// 60 minute average  (but only add values if we have enough samples... in this case approx 30 minutes)
			if ( (entryAgeMs <= ms60m) && (oldestAgeInMs > ms60m / 2) )
			{
				cnt60m++;
				sum60m += val;
			}

			// 30 minute average  (but only add values if we have enough samples... in this case approx 15 minutes)
			if ( (entryAgeMs <= ms30m) && (oldestAgeInMs > ms30m / 2) )
			{
				cnt30m++;
				sum30m += val;
			}

			// 15 minute average  (but only add values if we have enough samples... in this case approx 7.5 minutes)
			if ( (entryAgeMs <= ms15m) && (oldestAgeInMs > ms15m / 2) )
			{
				cnt15m++;
				sum15m += val;
			}

			// 5 minute average (but only add values if we have enough samples... in this case approx 2.5 minutes)
			if ( (entryAgeMs <= ms5m) && (oldestAgeInMs > ms5m / 2) )
			{
				cnt5m++;
				sum5m += val;
			}
		}
		
		// on: 5,15,30,60 minute: calculate and set scale to 2
		_osLoadAverage1min  = osLoadAverage1min;
		_osLoadAverage5min  = cnt5m  == 0 ? -1 : new BigDecimal( sum5m  / (cnt5m  * 1.0) ).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
		_osLoadAverage15min = cnt15m == 0 ? -1 : new BigDecimal( sum15m / (cnt15m * 1.0) ).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
		_osLoadAverage30min = cnt30m == 0 ? -1 : new BigDecimal( sum30m / (cnt30m * 1.0) ).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
		_osLoadAverage60min = cnt60m == 0 ? -1 : new BigDecimal( sum60m / (cnt60m * 1.0) ).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();

		_osLoadAverageAdjusted1min  =                    new BigDecimal( osLoadAverage1min / numOfProcs         ).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();;
		_osLoadAverageAdjusted5min  = cnt5m  == 0 ? -1 : new BigDecimal( (sum5m  / (cnt5m  * 1.0)) / numOfProcs ).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
		_osLoadAverageAdjusted15min = cnt15m == 0 ? -1 : new BigDecimal( (sum15m / (cnt15m * 1.0)) / numOfProcs ).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
		_osLoadAverageAdjusted30min = cnt30m == 0 ? -1 : new BigDecimal( (sum30m / (cnt30m * 1.0)) / numOfProcs ).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
		_osLoadAverageAdjusted60min = cnt60m == 0 ? -1 : new BigDecimal( (sum60m / (cnt60m * 1.0)) / numOfProcs ).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
	}

	/** Small Class to keep OsLoadAverage history values... so we can calculate 5 and 15 minute values */
	private static class OsLoadAvgEntry
	{
		long   _ts;
		double _val;
		public OsLoadAvgEntry(long ts, double val)
		{
			_ts  = ts;
			_val = val;
		}
	}

	/** Create a HTML Image, with a chart that shows: 1, 5, 15, 30 and 60 minutes values */
	private String createChart(String groupName, String label)
	{
		String htmlChartImage = MovingAverageChart.getChartAsHtmlImage(label, // Note make the chart on 1 hour to see more info
				MovingAverageCounterManager.getInstance(groupName, "1Min",  60),
				MovingAverageCounterManager.getInstance(groupName, "5Min",  60),
				MovingAverageCounterManager.getInstance(groupName, "15Min", 60),
				MovingAverageCounterManager.getInstance(groupName, "30Min", 60),
				MovingAverageCounterManager.getInstance(groupName, "60Min", 60));
		return htmlChartImage;
	}

	private void checkForAlarms()
	{
		if ( ! AlarmHandler.hasInstance() )
			return;

		Configuration conf = Configuration.getCombinedConfiguration();
		AlarmHandler ah = AlarmHandler.getInstance();
		
		String groupName1 = "H2WriterStat";
		String groupName2 = "H2WriterStat-Adjusted";

		double loadAverage_1m_threshold          = conf.getDoubleProperty(PROPKEY_AlarmOsLoadAverage1m,          DEFAULT_AlarmOsLoadAverage1m);
		double loadAverage_5m_threshold          = conf.getDoubleProperty(PROPKEY_AlarmOsLoadAverage5m,          DEFAULT_AlarmOsLoadAverage5m);
		double loadAverage_15m_threshold         = conf.getDoubleProperty(PROPKEY_AlarmOsLoadAverage15m,         DEFAULT_AlarmOsLoadAverage15m);
		double loadAverage_30m_threshold         = conf.getDoubleProperty(PROPKEY_AlarmOsLoadAverage30m,         DEFAULT_AlarmOsLoadAverage30m);
		double loadAverage_60m_threshold         = conf.getDoubleProperty(PROPKEY_AlarmOsLoadAverage60m,         DEFAULT_AlarmOsLoadAverage60m);

		double loadAverageAdjusted_1m_threshold  = conf.getDoubleProperty(PROPKEY_AlarmOsLoadAverageAdjusted1m,  DEFAULT_AlarmOsLoadAverageAdjusted1m);
		double loadAverageAdjusted_5m_threshold  = conf.getDoubleProperty(PROPKEY_AlarmOsLoadAverageAdjusted5m,  DEFAULT_AlarmOsLoadAverageAdjusted5m);
		double loadAverageAdjusted_15m_threshold = conf.getDoubleProperty(PROPKEY_AlarmOsLoadAverageAdjusted15m, DEFAULT_AlarmOsLoadAverageAdjusted15m);
		double loadAverageAdjusted_30m_threshold = conf.getDoubleProperty(PROPKEY_AlarmOsLoadAverageAdjusted30m, DEFAULT_AlarmOsLoadAverageAdjusted30m);
		double loadAverageAdjusted_60m_threshold = conf.getDoubleProperty(PROPKEY_AlarmOsLoadAverageAdjusted60m, DEFAULT_AlarmOsLoadAverageAdjusted60m);
		
		// Set hostname to servername@OsHostName 
		String srvName = System.getProperty("SERVERNAME");
		if (StringUtil.isNullOrBlank(srvName))
			srvName = Configuration.getCombinedConfiguration().getProperty("SERVERNAME");
		if (StringUtil.isNullOrBlank(srvName))
			srvName = Version.getAppName();
		
		String serviceInfoName = "H2WriterStat";
		String hostname        = srvName + "@" + StringUtil.getHostname();


		double threshold;

		// Should we set Alarm TimeToLive here... based on the PCS _lastConsumeTime * 1.5 or similar
		long timeToLive = -1;
		if (DbxTuneCentral.APP_NAME.equals(Version.getAppName()))
		{
			// if we are DbxCentral
			if (CentralPcsWriterHandler.hasInstance() && CentralPcsWriterHandler.getInstance().getMaxConsumeTime() > 0)
				timeToLive = CentralPcsWriterHandler.getInstance().getMaxConsumeTime();
		}
		else
		{
			// if we are any collector (xxxTune)
			if (PersistentCounterHandler.hasInstance() && PersistentCounterHandler.getInstance().getMaxConsumeTime() > 0)
				timeToLive = PersistentCounterHandler.getInstance().getMaxConsumeTime();
		}

        // Add counters to a in-memory-storage, so we can "graph" them on Alarms
        MovingAverageCounterManager.getInstance(groupName1, "1Min",  60).add(_osLoadAverage1min);
        MovingAverageCounterManager.getInstance(groupName1, "5Min",  60).add(_osLoadAverage5min);
        MovingAverageCounterManager.getInstance(groupName1, "15Min", 60).add(_osLoadAverage15min);
        MovingAverageCounterManager.getInstance(groupName1, "30Min", 60).add(_osLoadAverage30min);
        MovingAverageCounterManager.getInstance(groupName1, "60Min", 60).add(_osLoadAverage60min);

        MovingAverageCounterManager.getInstance(groupName2, "1Min",  60).add(_osLoadAverageAdjusted1min);
        MovingAverageCounterManager.getInstance(groupName2, "5Min",  60).add(_osLoadAverageAdjusted5min);
        MovingAverageCounterManager.getInstance(groupName2, "15Min", 60).add(_osLoadAverageAdjusted15min);
        MovingAverageCounterManager.getInstance(groupName2, "30Min", 60).add(_osLoadAverageAdjusted30min);
        MovingAverageCounterManager.getInstance(groupName2, "60Min", 60).add(_osLoadAverageAdjusted60min);
        
		//---------------------------------------------
		// Below is NORMAL Load Average, meaning: LoadAverage over ALL CPU's
		//---------------------------------------------

		// 1 minute
		threshold = loadAverage_1m_threshold;
		if (_osLoadAverage1min > threshold && isSystemAlarmsForColumnEnabledAndInTimeRange("osLoadAverage1min"))
		{
			AlarmEvent ae = new AlarmEventOsLoadAverage(serviceInfoName, threshold, hostname, AlarmEventOsLoadAverage.RangeType.RANGE_1_MINUTE, _osLoadAverage1min, _osLoadAverage5min, _osLoadAverage15min, _osLoadAverage30min, _osLoadAverage60min);
			ae.setTimeToLive(timeToLive);
			ae.setExtendedDescription(null, createChart(groupName1, "Load Average (1 hour)"));
			ah.addAlarm(ae);
		}

		// 5 minute
		threshold = loadAverage_5m_threshold;
		if (_osLoadAverage5min > threshold && isSystemAlarmsForColumnEnabledAndInTimeRange("osLoadAverage5min"))
		{
			AlarmEvent ae = new AlarmEventOsLoadAverage(serviceInfoName, threshold, hostname, AlarmEventOsLoadAverage.RangeType.RANGE_5_MINUTE, _osLoadAverage1min, _osLoadAverage5min, _osLoadAverage15min, _osLoadAverage30min, _osLoadAverage60min);
			ae.setTimeToLive(timeToLive);
			ae.setExtendedDescription(null, createChart(groupName1, "Load Average (1 hour)"));
			ah.addAlarm(ae);
		}

		// 15 minute
		threshold = loadAverage_15m_threshold;
		if (_osLoadAverage15min > threshold && isSystemAlarmsForColumnEnabledAndInTimeRange("osLoadAverage15min"))
		{
			AlarmEvent ae = new AlarmEventOsLoadAverage(serviceInfoName, threshold, hostname, AlarmEventOsLoadAverage.RangeType.RANGE_15_MINUTE, _osLoadAverage1min, _osLoadAverage5min, _osLoadAverage15min, _osLoadAverage30min, _osLoadAverage60min);
			ae.setTimeToLive(timeToLive);
			ae.setExtendedDescription(null, createChart(groupName1, "Load Average (1 hour)"));
			ah.addAlarm(ae);
		}

		// 30 minute
		threshold = loadAverage_30m_threshold;
		if (_osLoadAverage30min > threshold && isSystemAlarmsForColumnEnabledAndInTimeRange("osLoadAverage30min"))
		{
			AlarmEvent ae = new AlarmEventOsLoadAverage(serviceInfoName, threshold, hostname, AlarmEventOsLoadAverage.RangeType.RANGE_30_MINUTE, _osLoadAverage1min, _osLoadAverage5min, _osLoadAverage15min, _osLoadAverage30min, _osLoadAverage60min);
			ae.setTimeToLive(timeToLive);
			ae.setExtendedDescription(null, createChart(groupName1, "Load Average (1 hour)"));
			ah.addAlarm(ae);
		}

		// 60 minute
		threshold = loadAverage_60m_threshold;
		if (_osLoadAverage60min > threshold && isSystemAlarmsForColumnEnabledAndInTimeRange("osLoadAverage60min"))
		{
			AlarmEvent ae = new AlarmEventOsLoadAverage(serviceInfoName, threshold, hostname, AlarmEventOsLoadAverage.RangeType.RANGE_60_MINUTE, _osLoadAverage1min, _osLoadAverage5min, _osLoadAverage15min, _osLoadAverage30min, _osLoadAverage60min);
			ae.setTimeToLive(timeToLive);
			ae.setExtendedDescription(null, createChart(groupName1, "Load Average (1 hour)"));
			ah.addAlarm(ae);
		}

	
	
		//---------------------------------------------
		// Below is ADJUSTED Load Average, meaning: LoadAverage / numOfProcs
		//---------------------------------------------
		
		// 1 minute
		threshold = loadAverageAdjusted_1m_threshold;
		if (_osLoadAverageAdjusted1min > threshold && isSystemAlarmsForColumnEnabledAndInTimeRange("osLoadAverageAdjusted1min"))
		{
			AlarmEvent ae = new AlarmEventOsLoadAverageAdjusted(serviceInfoName, threshold, hostname, AlarmEventOsLoadAverageAdjusted.RangeType.RANGE_1_MINUTE, _osLoadAverageAdjusted1min, _osLoadAverageAdjusted5min, _osLoadAverageAdjusted15min, _osLoadAverageAdjusted30min, _osLoadAverageAdjusted60min);
			ae.setTimeToLive(timeToLive);
			ae.setExtendedDescription(null, createChart(groupName2, "Adjusted Load Average (1 hour)"));
			ah.addAlarm(ae);
		}

		// 5 minute
		threshold = loadAverageAdjusted_5m_threshold;
		if (_osLoadAverageAdjusted5min > threshold && isSystemAlarmsForColumnEnabledAndInTimeRange("osLoadAverageAdjusted5min"))
		{
			AlarmEvent ae = new AlarmEventOsLoadAverageAdjusted(serviceInfoName, threshold, hostname, AlarmEventOsLoadAverageAdjusted.RangeType.RANGE_5_MINUTE, _osLoadAverageAdjusted1min, _osLoadAverageAdjusted5min, _osLoadAverageAdjusted15min, _osLoadAverageAdjusted30min, _osLoadAverageAdjusted60min);
			ae.setTimeToLive(timeToLive);
			ae.setExtendedDescription(null, createChart(groupName2, "Adjusted Load Average (1 hour)"));
			ah.addAlarm(ae);
		}

		// 15 minute
		threshold = loadAverageAdjusted_15m_threshold;
		if (_osLoadAverageAdjusted15min > threshold && isSystemAlarmsForColumnEnabledAndInTimeRange("osLoadAverageAdjusted15min"))
		{
			AlarmEvent ae = new AlarmEventOsLoadAverageAdjusted(serviceInfoName, threshold, hostname, AlarmEventOsLoadAverageAdjusted.RangeType.RANGE_15_MINUTE, _osLoadAverageAdjusted1min, _osLoadAverageAdjusted5min, _osLoadAverageAdjusted15min, _osLoadAverageAdjusted30min, _osLoadAverageAdjusted60min);
			ae.setTimeToLive(timeToLive);
			ae.setExtendedDescription(null, createChart(groupName2, "Adjusted Load Average (1 hour)"));
			ah.addAlarm(ae);
		}

		// 30 minute
		threshold = loadAverageAdjusted_30m_threshold;
		if (_osLoadAverageAdjusted30min > threshold && isSystemAlarmsForColumnEnabledAndInTimeRange("osLoadAverageAdjusted30min"))
		{
			AlarmEvent ae = new AlarmEventOsLoadAverageAdjusted(serviceInfoName, threshold, hostname, AlarmEventOsLoadAverageAdjusted.RangeType.RANGE_30_MINUTE, _osLoadAverageAdjusted1min, _osLoadAverageAdjusted5min, _osLoadAverageAdjusted15min, _osLoadAverageAdjusted30min, _osLoadAverageAdjusted60min);
			ae.setTimeToLive(timeToLive);
			ae.setExtendedDescription(null, createChart(groupName2, "Adjusted Load Average (1 hour)"));
			ah.addAlarm(ae);
		}

		// 60 minute
		threshold = loadAverageAdjusted_60m_threshold;
		if (_osLoadAverageAdjusted60min > threshold && isSystemAlarmsForColumnEnabledAndInTimeRange("osLoadAverageAdjusted60min"))
		{
			AlarmEvent ae = new AlarmEventOsLoadAverageAdjusted(serviceInfoName, threshold, hostname, AlarmEventOsLoadAverageAdjusted.RangeType.RANGE_60_MINUTE, _osLoadAverageAdjusted1min, _osLoadAverageAdjusted5min, _osLoadAverageAdjusted15min, _osLoadAverageAdjusted30min, _osLoadAverageAdjusted60min);
			ae.setTimeToLive(timeToLive);
			ae.setExtendedDescription(null, createChart(groupName2, "Adjusted Load Average (1 hour)"));
			ah.addAlarm(ae);
		}
	}


	public static final String  PROPKEY_ALARM_isSystemAlarmsForColumnEnabled = "H2Writer.alarm.system.enabled.<COLNAME>";
	public static final boolean DEFAULT_ALARM_isSystemAlarmsForColumnEnabled = true;

	public static final String  PROPKEY_ALARM_isSystemAlarmsForColumnInTimeRange = "H2Writer.alarm.system.enabled.<COLNAME>.timeRange.cron";
//	public static final String  DEFAULT_ALARM_isSystemAlarmsForColumnInTimeRange = "* * * * *";
	public static final String  DEFAULT_ALARM_isSystemAlarmsForColumnInTimeRange = "!* 00-02 * * *";   // always allowed (except between 00:00 - 02:00, or first 2 hours on the day where a lot of Daily Summary Reports will be created)

	/** Is Alarms enabled or disabled */
	private boolean isSystemAlarmsForColumnEnabledAndInTimeRange(String colname)
	{
		Configuration conf = Configuration.getCombinedConfiguration();

		// Is it configured at all for this "name"
		String propNameIsEnabled = PROPKEY_ALARM_isSystemAlarmsForColumnEnabled.replace("<COLNAME>", colname);
		boolean enabled = conf.getBooleanProperty(propNameIsEnabled, DEFAULT_ALARM_isSystemAlarmsForColumnEnabled);
		if ( ! enabled )
			return enabled;

		// Is it "within time" for this "name"
		String propName = PROPKEY_ALARM_isSystemAlarmsForColumnInTimeRange.replace("<COLNAME>", colname);
		String cronStr = conf.getProperty(propName, DEFAULT_ALARM_isSystemAlarmsForColumnInTimeRange);

		// Uncomment if the default is "* * * * *" or *ALWAYS* allowed, otherwise we need to evaluate the crontab entry
		//if (DEFAULT_ALARM_isSystemAlarmsForColumnInTimeRange.equals(cronStr))
		//	return enabled;

		// is this a negation/not within time period...
		boolean negation = false;
		if (cronStr.startsWith("!"))
		{
			negation = true;
			cronStr  = cronStr.substring(1);
		}

		try
		{
			SchedulingPattern sp = new SchedulingPattern(cronStr);
			enabled = sp.match(System.currentTimeMillis());

			// if cron started with a '!' then invert the boolean
			if (negation)
				enabled = ! enabled;
		}
		catch(InvalidPatternException ex)
		{
			_logger.error("The specified 'cron' value '" + cronStr + "' for property '" + propName + "' is not a valid cron-pattern. This will be disregarded. Caught: " + ex);
		}

		return enabled;
	}

	private void printAlarmConfig()
	{
		Configuration conf = Configuration.getCombinedConfiguration();

		_logger.info("System Defined Alarms properties are listed below for 'H2WriterStat'.");
		String  prefix = "       ";

		String[] names = {"osLoadAverage1min"        , "osLoadAverage5min"        , "osLoadAverage15min"        , "osLoadAverage30min"        , "osLoadAverage60min", 
		                  "osLoadAverageAdjusted1min", "osLoadAverageAdjusted5min", "osLoadAverageAdjusted15min", "osLoadAverageAdjusted30min", "osLoadAverageAdjusted60min"};

		for (String colname : names)
		{
			String propName = PROPKEY_ALARM_isSystemAlarmsForColumnEnabled.replace("<COLNAME>", colname);
			String propVal = conf.getProperty(propName, DEFAULT_ALARM_isSystemAlarmsForColumnEnabled+"");

			// At what times can the alarms be triggered
			String cronProp = PROPKEY_ALARM_isSystemAlarmsForColumnInTimeRange.replace("<COLNAME>", colname);
			String cronPat = conf.getProperty(propName, DEFAULT_ALARM_isSystemAlarmsForColumnInTimeRange);

			// Check that the 'cron' scheduling pattern is valid...
			String cronPatStr = cronPat;
			if (cronPatStr.startsWith("!"))
			{
				cronPatStr = cronPatStr.substring(1);
			}
			if ( ! SchedulingPattern.validate(cronPatStr) )
				_logger.error("The cron scheduling pattern '"+cronPatStr+"' is NOT VALID. for the property '" + cronProp + "', this will not be used at runtime, furter warnings will also be issued.");

			String cronPatDesc = CronUtils.getCronExpressionDescriptionForAlarms(cronPat);

			_logger.info(prefix + propName + " = " + propVal);
			_logger.info(prefix + cronProp + " = " + cronPat + "   #-- desc: " + cronPatDesc);
		}
	}

	public String getH2FileName()
	{
		if (_h2DbFile == null)
			return null;
		
		return _h2DbFile.getAbsolutePath();
	}

	public String getH2FileNameShort()
	{
		if (_h2DbFile == null)
			return "";
		
		return FilenameUtils.getName(_h2DbFile.getAbsolutePath());
	}

	public BigDecimal getRateValue(String key)
	{
		BigDecimal val = null;
		
		if (_lastRate != null)
			val = _lastRate.get(key);
		
		if (val == null)
			val = new BigDecimal(0.0);

		return val;
	}

	public Long getDiffValue(String key)
	{
		Long val = null;
		
		if (_lastDiff != null)
			val = _lastDiff.get(key);
		
		if (val == null)
			val = Long.valueOf(0);

		return val;
	}

	public Long getAbsValue(String key)
	{
		Long val = null;
		
		if (_lastAbs != null)
			val = _lastAbs.get(key);
		
		if (val == null)
			val = Long.valueOf(0);

		return val;
	}

	//
	// Format of the log entry
	// H2-PerfCounters{sampleTime=00:22:53.143, OsLoadAvgAdj[1m=0.04, 5m=0.04, 15m=0.04, 30m=0.19, 60m=-1.00], FILE_READ[abs=222746, diff=100672, rate=73.3], FILE_WRITE[abs=758, diff=757, rate=0.6], PAGE_COUNT[abs=8452226, diff=151371, rate=110.2], H2_FILE_SIZE_KB[abs=33808904, diff=605484, rate=440.9], H2_FILE_SIZE_MB[abs=33016, diff=591, rate=0.4], H2_FILE_NAME='DBXTUNE_CENTRAL_DB.mv.db'}
	// 
	// A bit more friendy to read
	// H2-PerfCounters{
	//		sampleTime=00:22:53.143, 
	//		OsLoadAvgAdj[1m=0.04, 5m=0.04, 15m=0.04, 30m=0.19, 60m=-1.00], 
	//		FILE_READ[abs=222746, diff=100672, rate=73.3], 
	//		FILE_WRITE[abs=758, diff=757, rate=0.6], 
	//		PAGE_COUNT[abs=8452226, diff=151371, rate=110.2], 
	//		H2_FILE_SIZE_KB[abs=33808904, diff=605484, rate=440.9], 
	//		H2_FILE_SIZE_MB[abs=33016, diff=591, rate=0.4], 
	//		H2_FILE_NAME='DBXTUNE_CENTRAL_DB.mv.db'
	// }
	public static class StatEntry
	{
		public Timestamp logTs;           // the log file timestamp 
//		public Timestamp sampleTime;      // the log file timestamp 
		public long      sampleDurationInMs;  // in above example it's the "sampleTime"

		public double    OsLoadAvgAdj_1m;
		public double    OsLoadAvgAdj_5m;
		public double    OsLoadAvgAdj_15m;
		public double    OsLoadAvgAdj_30m;
		public double    OsLoadAvgAdj_60m;

		public int       FILE_READ_abs;
		public int       FILE_READ_diff;
		public double    FILE_READ_rate;

		public int       FILE_WRITE_abs;
		public int       FILE_WRITE_diff;
		public double    FILE_WRITE_rate;

		public int       PAGE_COUNT_abs;
		public int       PAGE_COUNT_diff;
		public double    PAGE_COUNT_rate;

		public int       H2_FILE_SIZE_KB_abs;
		public int       H2_FILE_SIZE_KB_diff;
		public double    H2_FILE_SIZE_KB_rate;

		public int       H2_FILE_SIZE_MB_abs;
		public int       H2_FILE_SIZE_MB_diff;
		public double    H2_FILE_SIZE_MB_rate;

		public String    H2_FILE_NAME;
	}
	public static List<StatEntry> parseStatFromLogFile(String filename, Timestamp startDate, Timestamp endDate, long minDuration)
	throws FileNotFoundException, IOException, ParseException
	{
		List<StatEntry> list = new ArrayList<>();
		
		// Files in the LOGDIR 
		File f = new File(DbxTuneCentral.getAppLogDir() + "/" + filename);
		
		try (BufferedReader br = new BufferedReader(new FileReader(f))) 
		{
			String line;
			while ((line = br.readLine()) != null) 
			{
				if (line.indexOf("H2-PerfCounters{") != -1)
				{
					StatEntry se = new StatEntry();
					
					String logTsStr = "";
					if (line.indexOf(" - ") != -1)
					{
						logTsStr = StringUtils.substringBefore(line, " - ");
						se.logTs = TimeUtils.parseToTimestamp(logTsStr, "yyyy-MM-dd HH:mm:ss,SSS");
					}
					
					if (startDate != null)
					{
						// skip to-early dates
						if (se.logTs.getTime() < startDate.getTime())
							continue;
					}
					
					if (endDate != null)
					{
						// skip to-late dates
						if (se.logTs.getTime() > endDate.getTime())
							continue;
					}
					
					String h2PerfCntEntry = StringUtils.substringBetween(line, "H2-PerfCounters{", "}. ");
					
					String sampleTime   = StringUtils.substringBetween(h2PerfCntEntry, "sampleTime=",    ",");
					String H2_FILE_NAME = StringUtils.substringBetween(h2PerfCntEntry, "H2_FILE_NAME='", "'");

					int colonCount = StringUtils.countMatches(sampleTime, ':');
					String dateFmt = "HH:mm:ss.SSS";
					if (colonCount == 1)
						dateFmt = "mm:ss.SSS";

					Timestamp ts = TimeUtils.parseToTimestamp(sampleTime, dateFmt);
					se.sampleDurationInMs = ts.getTime();


					String[] entries = StringUtils.substringsBetween(h2PerfCntEntry, "[", "]");
					if (entries.length == 6)
					{
						Map<String,String> OsLoadAvgAdj    = StringUtil.parseCommaStrToMap(entries[0]);
						Map<String,String> FILE_READ       = StringUtil.parseCommaStrToMap(entries[1]);
						Map<String,String> FILE_WRITE      = StringUtil.parseCommaStrToMap(entries[2]);
						Map<String,String> PAGE_COUNT      = StringUtil.parseCommaStrToMap(entries[3]);
						Map<String,String> H2_FILE_SIZE_KB = StringUtil.parseCommaStrToMap(entries[4]);
						Map<String,String> H2_FILE_SIZE_MB = StringUtil.parseCommaStrToMap(entries[5]);

						String defVal = "-1";
						
						se.OsLoadAvgAdj_1m      = Double.valueOf(OsLoadAvgAdj.getOrDefault("1m", defVal));
						se.OsLoadAvgAdj_5m      = Double.valueOf(OsLoadAvgAdj.getOrDefault("5m", defVal));
						se.OsLoadAvgAdj_15m     = Double.valueOf(OsLoadAvgAdj.getOrDefault("15m", defVal));
						se.OsLoadAvgAdj_30m     = Double.valueOf(OsLoadAvgAdj.getOrDefault("30m", defVal));
						se.OsLoadAvgAdj_60m     = Double.valueOf(OsLoadAvgAdj.getOrDefault("60m", defVal));

						se.FILE_READ_abs        = Integer.valueOf(FILE_READ.getOrDefault("abs", defVal));
						se.FILE_READ_diff       = Integer.valueOf(FILE_READ.getOrDefault("diff", defVal));
						se.FILE_READ_rate       = Double .valueOf(FILE_READ.getOrDefault("rate", defVal));

						se.FILE_WRITE_abs       = Integer.valueOf(FILE_WRITE.getOrDefault("abs", defVal));
						se.FILE_WRITE_diff      = Integer.valueOf(FILE_WRITE.getOrDefault("diff", defVal));
						se.FILE_WRITE_rate      = Double .valueOf(FILE_WRITE.getOrDefault("rate", defVal));

						se.PAGE_COUNT_abs       = Integer.valueOf(PAGE_COUNT.getOrDefault("abs", defVal));
						se.PAGE_COUNT_diff      = Integer.valueOf(PAGE_COUNT.getOrDefault("diff", defVal));
						se.PAGE_COUNT_rate      = Double .valueOf(PAGE_COUNT.getOrDefault("rate", defVal));

						se.H2_FILE_SIZE_KB_abs  = Integer.valueOf(H2_FILE_SIZE_KB.getOrDefault("abs", defVal));
						se.H2_FILE_SIZE_KB_diff = Integer.valueOf(H2_FILE_SIZE_KB.getOrDefault("diff", defVal));
						se.H2_FILE_SIZE_KB_rate = Double .valueOf(H2_FILE_SIZE_KB.getOrDefault("rate", defVal));

						se.H2_FILE_SIZE_MB_abs  = Integer.valueOf(H2_FILE_SIZE_MB.getOrDefault("abs", defVal));
						se.H2_FILE_SIZE_MB_diff = Integer.valueOf(H2_FILE_SIZE_MB.getOrDefault("diff", defVal));
						se.H2_FILE_SIZE_MB_rate = Double .valueOf(H2_FILE_SIZE_MB.getOrDefault("rate", defVal));

						se.H2_FILE_NAME = H2_FILE_NAME;
						
						list.add(se);
					}
				}
			}
		}
		
		return list;
	}

	public String getStatString()
	{
		if (_lastRefresh == -1)
			return "";

		if (_lastIntervallInMs < 0)
			_lastIntervallInMs = 0;
		
		if (_lastIntervallInMs == 0)
			return "";
		
		String osLoadAvg    = "";
		if (_osLoadAverage1min != -1) // Note: NOT supported on some OS (Windows), so do not print it
			osLoadAvg    = String.format("OsLoadAvg[1m=%.2f, 5m=%.2f, 15m=%.2f, 30m=%.2f, 60m=%.2f], ", _osLoadAverage1min, _osLoadAverage5min, _osLoadAverage15min, _osLoadAverage30min, _osLoadAverage60min);
		
		String osLoadAvgAdj = "";
		if (_osLoadAverage1min != -1) // Note: NOT supported on some OS (Windows), so do not print it
			osLoadAvgAdj = String.format("OsLoadAvgAdj[1m=%.2f, 5m=%.2f, 15m=%.2f, 30m=%.2f, 60m=%.2f], ", _osLoadAverageAdjusted1min, _osLoadAverageAdjusted5min, _osLoadAverageAdjusted15min, _osLoadAverageAdjusted30min, _osLoadAverageAdjusted60min);
		
		String prefix       = "H2-PerfCounters{sampleTime=" + TimeUtils.msToTimeStr(getSampleTimeFormat(), _lastIntervallInMs) + ", ";
//		String osLoadAvg    = "MxOsLoadAvg"     + "[1m="  + _osLoadAverage1min           + ", 5m="   + _osLoadAverage5min            + ", 15m="  + _osLoadAverage15min           + "], ";
		String fileRead     = "FILE_READ"       + "[abs=" + getAbsValue(FILE_READ)       + ", diff=" + getDiffValue(FILE_READ)       + ", rate=" + getRateValue(FILE_READ)       + "], ";
		String fileWrite    = "FILE_WRITE"      + "[abs=" + getAbsValue(FILE_WRITE)      + ", diff=" + getDiffValue(FILE_WRITE)      + ", rate=" + getRateValue(FILE_WRITE)      + "], ";
		String pageCount    = "PAGE_COUNT"      + "[abs=" + getAbsValue(PAGE_COUNT)      + ", diff=" + getDiffValue(PAGE_COUNT)      + ", rate=" + getRateValue(PAGE_COUNT)      + "], ";
		String h2FileSizeKb = "H2_FILE_SIZE_KB" + "[abs=" + getAbsValue(H2_FILE_SIZE_KB) + ", diff=" + getDiffValue(H2_FILE_SIZE_KB) + ", rate=" + getRateValue(H2_FILE_SIZE_KB) + "], ";
		String h2FileSizeMb = "H2_FILE_SIZE_MB" + "[abs=" + getAbsValue(H2_FILE_SIZE_MB) + ", diff=" + getDiffValue(H2_FILE_SIZE_MB) + ", rate=" + getRateValue(H2_FILE_SIZE_MB) + "], ";
		String postFix      = "H2_FILE_NAME"    + "='" + getH2FileNameShort() + "'}. ";
		
		// Remember max length for next print
		_strMaxLen_prefix       = Math.max(_strMaxLen_prefix      , prefix      .length());
		_strMaxLen_osLoadAvg    = Math.max(_strMaxLen_osLoadAvg   , osLoadAvg   .length());
		_strMaxLen_osLoadAvgAdj = Math.max(_strMaxLen_osLoadAvgAdj, osLoadAvgAdj.length());
		_strMaxLen_fileRead     = Math.max(_strMaxLen_fileRead    , fileRead    .length());
		_strMaxLen_fileWrite    = Math.max(_strMaxLen_fileWrite   , fileWrite   .length());
		_strMaxLen_pageCount    = Math.max(_strMaxLen_pageCount   , pageCount   .length());
		_strMaxLen_h2FileSizeKb = Math.max(_strMaxLen_h2FileSizeKb, h2FileSizeKb.length());
		_strMaxLen_h2FileSizeMb = Math.max(_strMaxLen_h2FileSizeMb, h2FileSizeMb.length());
		_strMaxLen_postFix      = Math.max(_strMaxLen_postFix     , postFix     .length());

		// Add space at the end to of each section. (for readability)
		prefix       = StringUtil.left(prefix      , _strMaxLen_prefix      );
		osLoadAvg    = StringUtil.left(osLoadAvg   , _strMaxLen_osLoadAvg   );
		osLoadAvgAdj = StringUtil.left(osLoadAvgAdj, _strMaxLen_osLoadAvgAdj);
		fileRead     = StringUtil.left(fileRead    , _strMaxLen_fileRead    );
		fileWrite    = StringUtil.left(fileWrite   , _strMaxLen_fileWrite   );
		pageCount    = StringUtil.left(pageCount   , _strMaxLen_pageCount   );
		h2FileSizeKb = StringUtil.left(h2FileSizeKb, _strMaxLen_h2FileSizeKb);
		h2FileSizeMb = StringUtil.left(h2FileSizeMb, _strMaxLen_h2FileSizeMb);
		postFix      = StringUtil.left(postFix     , _strMaxLen_postFix     );

		//String osLoad = osLoadAvg;     // use 'osLoadAvg' if you want to see the whole machine, which can lie a bit
		String osLoad = osLoadAvgAdj;    // use 'osLoadAvgAdj' if you want to see the load average per CPU/Core
		
		return prefix + osLoad + fileRead + fileWrite + pageCount + h2FileSizeKb + h2FileSizeMb + postFix;
	}
	// used to format the length of the string
	private int _strMaxLen_prefix       = 0;
	private int _strMaxLen_osLoadAvg    = 0;
	private int _strMaxLen_osLoadAvgAdj = 0;
	private int _strMaxLen_fileRead     = 0;
	private int _strMaxLen_fileWrite    = 0;
	private int _strMaxLen_pageCount    = 0;
	private int _strMaxLen_h2FileSizeKb = 0;
	private int _strMaxLen_h2FileSizeMb = 0;
	private int _strMaxLen_postFix      = 0;


	
	//-----------------------------------------------------------------------
	//-----------------------------------------------------------------------
	// HELPER ConnectionProvider CLASSES for: PersistWriter & CentralPersistWriter
	//-----------------------------------------------------------------------
	//-----------------------------------------------------------------------

	/**
	 * Get a connection from the Central Persist Reader, and return it to the connection pool
	 */
	public static class CentralH2PerfCounterConnectionProvider implements ConnectionProvider
	{
		@Override
		public DbxConnection getConnection()
		{
			if (CentralPersistReader.hasInstance())
			{
				try
				{
					return CentralPersistReader.getInstance().getConnection();
				}
				catch (SQLException ex)
				{
					_logger.info("Central Persist Writer Connection Provider has problems getting a connection. Caught: "+ex);
				}
			}
			return null;
		}

		@Override
		public void releaseConnection(DbxConnection conn)
		{
			if (CentralPersistReader.hasInstance())
				CentralPersistReader.getInstance().releaseConnection(conn);
		}
		
		@Override
		public DbxConnection getNewConnection(String appname)
		{
			throw new RuntimeException("getNewConnection(appname): -NOT-IMPLEMENTED-");
		}
	}
	

	/**
	 * Get a connection from the DbxTune Persist Reader
	 */
	public static class DbxTuneH2PerfCounterConnectionProvider implements ConnectionProvider
	{
		@Override
		public DbxConnection getConnection()
		{
			if (PersistReader.hasInstance())
			{
//				return PersistReader.getInstance().getConnection();
				
			}
			return null;
		}

		@Override
		public void releaseConnection(DbxConnection conn)
		{
		}
		
		@Override
		public DbxConnection getNewConnection(String appname)
		{
			throw new RuntimeException("getNewConnection(appname): -NOT-IMPLEMENTED-");
		}

	}

}
