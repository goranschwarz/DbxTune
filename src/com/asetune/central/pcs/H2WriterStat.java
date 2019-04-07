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
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import com.asetune.pcs.PersistReader;
import com.asetune.sql.conn.DbxConnection;
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

	private static final String     DEFAULT_sampleTimeFormat = "%?DD[d ]%?HH[:]%MM:%SS.%ms";

	private String                  _sampleTimeFormat = DEFAULT_sampleTimeFormat;
	private long                    _lastRefresh = -1;
	private long                    _lastIntervallInMs = -1;
	private Map<String, Long>       _lastAbs  = null;
	private Map<String, Long>       _lastDiff = new HashMap<>();
	private Map<String, BigDecimal> _lastRate = new HashMap<>();
	
	private ConnectionProvider _connProvider = null;
	private File _h2DbFile = null;
	
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
		
		_lastRefresh = System.currentTimeMillis();
		return this;
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
		
		String prefix       = "H2-PerfCounters{sampleTime=" + TimeUtils.msToTimeStr(getSampleTimeFormat(), _lastIntervallInMs) + ", ";
		String fileRead     = "FILE_READ"       + "[abs=" + getAbsValue(FILE_READ)       + ", diff=" + getDiffValue(FILE_READ)       + ", rate=" + getRateValue(FILE_READ)       + "], ";
		String fileWrite    = "FILE_WRITE"      + "[abs=" + getAbsValue(FILE_WRITE)      + ", diff=" + getDiffValue(FILE_WRITE)      + ", rate=" + getRateValue(FILE_WRITE)      + "], ";
		String pageCount    = "PAGE_COUNT"      + "[abs=" + getAbsValue(PAGE_COUNT)      + ", diff=" + getDiffValue(PAGE_COUNT)      + ", rate=" + getRateValue(PAGE_COUNT)      + "], ";
		String h2FileSizeKb = "H2_FILE_SIZE_KB" + "[abs=" + getAbsValue(H2_FILE_SIZE_KB) + ", diff=" + getDiffValue(H2_FILE_SIZE_KB) + ", rate=" + getRateValue(H2_FILE_SIZE_KB) + "], ";
		String h2FileSizeMb = "H2_FILE_SIZE_MB" + "[abs=" + getAbsValue(H2_FILE_SIZE_MB) + ", diff=" + getDiffValue(H2_FILE_SIZE_MB) + ", rate=" + getRateValue(H2_FILE_SIZE_MB) + "], ";
		String postFix      = "H2_FILE_NAME"    + "='" + getH2FileNameShort() + "'}. ";
		
		// Remember max length for next print
		_strMaxLen_prefix       = Math.max(_strMaxLen_prefix      , prefix      .length());
		_strMaxLen_fileRead     = Math.max(_strMaxLen_fileRead    , fileRead    .length());
		_strMaxLen_fileWrite    = Math.max(_strMaxLen_fileWrite   , fileWrite   .length());
		_strMaxLen_pageCount    = Math.max(_strMaxLen_pageCount   , pageCount   .length());
		_strMaxLen_h2FileSizeKb = Math.max(_strMaxLen_h2FileSizeKb, h2FileSizeKb.length());
		_strMaxLen_h2FileSizeMb = Math.max(_strMaxLen_h2FileSizeMb, h2FileSizeMb.length());
		_strMaxLen_postFix      = Math.max(_strMaxLen_postFix     , postFix     .length());

		// Add space at the end to of each section. (for readability)
		prefix       = StringUtil.left(prefix      , _strMaxLen_prefix      );
		fileRead     = StringUtil.left(fileRead    , _strMaxLen_fileRead    );
		fileWrite    = StringUtil.left(fileWrite   , _strMaxLen_fileWrite   );
		pageCount    = StringUtil.left(pageCount   , _strMaxLen_pageCount   );
		h2FileSizeKb = StringUtil.left(h2FileSizeKb, _strMaxLen_h2FileSizeKb);
		h2FileSizeMb = StringUtil.left(h2FileSizeMb, _strMaxLen_h2FileSizeMb);
		postFix      = StringUtil.left(postFix     , _strMaxLen_postFix     );
		
		return prefix + fileRead + fileWrite + pageCount + h2FileSizeKb + h2FileSizeMb + postFix;
	}
	// used to format the length of the string
	private int _strMaxLen_prefix       = 0;
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
