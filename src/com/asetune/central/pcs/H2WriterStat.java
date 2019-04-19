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
package com.asetune.central.pcs;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.math.ec.ECCurve.Config;

import com.asetune.Version;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.events.AlarmEventOsLoadAverage;
import com.asetune.pcs.PersistReader;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.ConnectionProvider;
import com.asetune.utils.H2UrlHelper;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;

/**
 * Small dummy implementation to keep track of H2 counter statistics
 * <ul>
 *   <li>info.FILE_READ</li>
 *   <li>info.FILE_WRITE</li>
 *   <li>info.PAGE_COUNT</li>
 *   <li>and possibly more</li>
 * </ul>
 *  
 *  I could have used a CounterModel for this but I decided to do it this way instead...
 *
 * @author gorans
 *
 */
public class H2WriterStat
{
	private static Logger _logger = Logger.getLogger(H2WriterStat.class);

	// implements singleton pattern
	private static H2WriterStat _instance = null;

	public static final String      PROPKEY_sampleOsLoadAverage   = "H2WriterStat.sample.osLoadAverage";
	public static final boolean     DEFAULT_sampleOsLoadAverage   = true;

	public static final String      PROPKEY_AlarmOsLoadAverage1m  = "H2WriterStat.alarm.osLoadAverage.1m.gt";
	public static final Double      DEFAULT_AlarmOsLoadAverage1m  = 99.0; // more or less DISABLED

	public static final String      PROPKEY_AlarmOsLoadAverage5m  = "H2WriterStat.alarm.osLoadAverage.5m.gt";
	public static final Double      DEFAULT_AlarmOsLoadAverage5m  = 99.0; // more or less DISABLED

	public static final String      PROPKEY_AlarmOsLoadAverage15m = "H2WriterStat.alarm.osLoadAverage.15m.gt";
	public static final Double      DEFAULT_AlarmOsLoadAverage15m = 4.0;

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
	public static H2WriterStat getInstance()
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
		String sql = "select NAME, cast(VALUE as bigint) as VALUE from INFORMATION_SCHEMA.SETTINGS where NAME in ('"+FILE_READ+"', '"+FILE_WRITE+"', '"+PAGE_COUNT+"')";

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
			calcLoadAverage();
			
			checkForAlarms();
		}

		_lastRefresh = System.currentTimeMillis();
		return this;
	}

	private void calcLoadAverage()
	{
		OperatingSystemMXBean os  = ManagementFactory.getOperatingSystemMXBean();
		double osLoadAverage1min  = os.getSystemLoadAverage();
		long now = System.currentTimeMillis();
	
		// if NOT SUPPORTED, do nothing...
//		if (osLoadAverage1min == -1)
//			return;
		
		if (_osLoadAverageList == null)
			_osLoadAverageList = new LinkedList<>();

		// Calculate 5 and 15 minute
		long   ms5m   = 300_000; // 5 * 60 * 1000;
		int    cnt5m  = 0;
		double sum5m  = 0;

		long   ms15m  = 1500_000; // 15 * 60 * 1000;
		int    cnt15m = 0;
		double sum15m = 0;

		// Add current entry to the list
		_osLoadAverageList.add(new OsLoadAvgEntry(now, osLoadAverage1min));

		//
		// Get oldest entry so we can check if we have enough time/samples to calculate 5 and 15 minute values 
		//
		// 15 minute and 5 minute would typically be HIGH at start time (since it's the same value as 1 minute)... 
		// So we might want to "skip" those until we have enough data point to measure (or similar)
		// Otherwise we will have *ALARMS* that are "faulty".
		OsLoadAvgEntry oldestEntry = _osLoadAverageList.getFirst(); // note: new entries are added at "END", so oldest entry would be FIRST
		long oldestAgeInMs = -1;
		if (oldestEntry != null)
			oldestAgeInMs = now - oldestEntry._ts;

		// Loop the list and remove old entries
		// and calculate the Average for 5 and 15 minutes
		for(Iterator<OsLoadAvgEntry> i = _osLoadAverageList.iterator(); i.hasNext();) 
		{
			OsLoadAvgEntry entry = i.next();
			long   ts  = entry._ts;
			double val = entry._val;

			// Remove entries that are older than 15 minutes
			if ( (now-ts) > ms15m )
			{
				i.remove();
				continue;
			}
			
			// 15 minute average  (but only add values if we have enough samples... in this case approx 7.5 minutes)
			// Since we removed everything older than 15 minutes above... 
			// we can do calculation without an if statement here 
			if (oldestAgeInMs > ms15m / 2)
			{
				cnt15m++;
				sum15m += val;
			}

			
			// continue if older than 5 minutes
			if ( (now-ts) > ms5m )
				continue;

			// 5 minute average (but only add values if we have enough samples... in this case approx 2.5 minutes)
			if (oldestAgeInMs > ms5m / 2)
			{
				cnt5m++;
				sum5m += val;
			}
		}
		
		_osLoadAverage1min  = osLoadAverage1min;
		_osLoadAverage5min  = cnt5m  == 0 ? -1 : sum5m  / (cnt5m  * 1.0);
		_osLoadAverage15min = cnt15m == 0 ? -1 : sum15m / (cnt15m * 1.0);
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

	private void checkForAlarms()
	{
		if ( ! AlarmHandler.hasInstance() )
			return;

		Configuration conf = Configuration.getCombinedConfiguration();
		AlarmHandler ah = AlarmHandler.getInstance();
		
		double loadAverage_1m_threshold  = conf.getDoubleProperty(PROPKEY_AlarmOsLoadAverage1m,  DEFAULT_AlarmOsLoadAverage1m);
		double loadAverage_5m_threshold  = conf.getDoubleProperty(PROPKEY_AlarmOsLoadAverage5m,  DEFAULT_AlarmOsLoadAverage5m);
		double loadAverage_15m_threshold = conf.getDoubleProperty(PROPKEY_AlarmOsLoadAverage15m, DEFAULT_AlarmOsLoadAverage15m);

		// Set hostname to servername@OsHostName 
		String srvName = System.getProperty("SERVERNAME");
		if (StringUtil.isNullOrBlank(srvName))
			srvName = Configuration.getCombinedConfiguration().getProperty("SERVERNAME");
		if (StringUtil.isNullOrBlank(srvName))
			srvName = Version.getAppName();
		
		String serviceInfoName = "H2WriterStat";
		String hostname        = srvName + "@" + StringUtil.getHostname();


		// If any of 1,5,15 minute threshold are crossed... set scale to 2 (for nicer formatting)
		if (    _osLoadAverage1min  > loadAverage_1m_threshold
		     || _osLoadAverage5min  > loadAverage_5m_threshold
		     || _osLoadAverage15min > loadAverage_15m_threshold  )
		{
			_osLoadAverage1min  = new BigDecimal(_osLoadAverage1min).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
			_osLoadAverage5min  = new BigDecimal(_osLoadAverage1min).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
			_osLoadAverage15min = new BigDecimal(_osLoadAverage1min).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
		}

		
		double threshold;

		// 1 minute
		threshold = loadAverage_1m_threshold;
		if (_osLoadAverage1min > threshold)
		{
			AlarmEvent ae = new AlarmEventOsLoadAverage(serviceInfoName, threshold, hostname, AlarmEventOsLoadAverage.RangeType.RANGE_1_MINUTE, _osLoadAverage1min, _osLoadAverage5min, _osLoadAverage15min);
			ah.addAlarm(ae);
		}

		// 5 minute
		threshold = loadAverage_5m_threshold;
		if (_osLoadAverage5min > threshold)
		{
			AlarmEvent ae = new AlarmEventOsLoadAverage(serviceInfoName, threshold, hostname, AlarmEventOsLoadAverage.RangeType.RANGE_5_MINUTE, _osLoadAverage1min, _osLoadAverage5min, _osLoadAverage15min);
			ah.addAlarm(ae);
		}

		// 15 minute
		threshold = loadAverage_15m_threshold;
		if (_osLoadAverage15min > threshold)
		{
			AlarmEvent ae = new AlarmEventOsLoadAverage(serviceInfoName, threshold, hostname, AlarmEventOsLoadAverage.RangeType.RANGE_15_MINUTE, _osLoadAverage1min, _osLoadAverage5min, _osLoadAverage15min);
			ah.addAlarm(ae);
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
			val = new Long(0);

		return val;
	}

	public Long getAbsValue(String key)
	{
		Long val = null;
		
		if (_lastAbs != null)
			val = _lastAbs.get(key);
		
		if (val == null)
			val = new Long(0);

		return val;
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
			osLoadAvg    = String.format("OsLoadAvg[1m=%.2f, 5m=%.2f, 15m=%.2f], ", _osLoadAverage1min, _osLoadAverage5min, _osLoadAverage15min);
		
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
		_strMaxLen_fileRead     = Math.max(_strMaxLen_fileRead    , fileRead    .length());
		_strMaxLen_fileWrite    = Math.max(_strMaxLen_fileWrite   , fileWrite   .length());
		_strMaxLen_pageCount    = Math.max(_strMaxLen_pageCount   , pageCount   .length());
		_strMaxLen_h2FileSizeKb = Math.max(_strMaxLen_h2FileSizeKb, h2FileSizeKb.length());
		_strMaxLen_h2FileSizeMb = Math.max(_strMaxLen_h2FileSizeMb, h2FileSizeMb.length());
		_strMaxLen_postFix      = Math.max(_strMaxLen_postFix     , postFix     .length());

		// Add space at the end to of each section. (for readability)
		prefix       = StringUtil.left(prefix      , _strMaxLen_prefix      );
		osLoadAvg    = StringUtil.left(osLoadAvg   , _strMaxLen_osLoadAvg   );
		fileRead     = StringUtil.left(fileRead    , _strMaxLen_fileRead    );
		fileWrite    = StringUtil.left(fileWrite   , _strMaxLen_fileWrite   );
		pageCount    = StringUtil.left(pageCount   , _strMaxLen_pageCount   );
		h2FileSizeKb = StringUtil.left(h2FileSizeKb, _strMaxLen_h2FileSizeKb);
		h2FileSizeMb = StringUtil.left(h2FileSizeMb, _strMaxLen_h2FileSizeMb);
		postFix      = StringUtil.left(postFix     , _strMaxLen_postFix     );
		
		return prefix + osLoadAvg + fileRead + fileWrite + pageCount + h2FileSizeKb + h2FileSizeMb + postFix;
	}
	// used to format the length of the string
	private int _strMaxLen_prefix       = 0;
	private int _strMaxLen_osLoadAvg    = 0;
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
