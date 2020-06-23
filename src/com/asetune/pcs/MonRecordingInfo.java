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
package com.asetune.pcs;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.asetune.pcs.PersistReader.MonVersionInfo;
import com.asetune.pcs.report.content.RecordingInfo;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;
import com.asetune.utils.VersionShort;

public class MonRecordingInfo
{
	private static Logger _logger = Logger.getLogger(RecordingInfo.class);

	public Timestamp _sessionStartTime = null;
	
//	private String    _recAppName        = "";
//	private String    _recBuildString    = "";
//	private String    _recVersionString  = "";
	private String    _dbmsVersionString = "";
	private long      _dbmsVersionNum    = -1;
	private String    _dbmsServerName    = "";
	private Timestamp _dbmsStartTime     = null;

	public MonVersionInfo _monVersionInfo = null;

	
	public MonRecordingInfo(DbxConnection conn, Timestamp sessionStartTime)
	{
		initialize(conn, sessionStartTime);
	}
	
	/** Get the DBMS Version string stored by any of the DbxTune collectors */
	public String getDbmsVersionStr()
	{
		return _dbmsVersionString;
	}

	/** Get the DBMS Version string stored by any of the DbxTune collectors, and then parse it into a number */
	public long getDbmsVersionNum()
	{
		return _dbmsVersionNum;
	}

	/** Get the DBMS Server/instance name stored by any of the DbxTune collectors */
	public String getDbmsServerName()
	{
		return _dbmsServerName;
	}

	/** Get the DBMS Server/instance last start time */
	public Timestamp getDbmsStartTime()
	{
		return _dbmsStartTime;
	}

	/** Get the DBMS Server/instance last start time in days. @returns -1 if UNKNOWN. otherwise an INT value */
	public int getDbmsStartTimeInDays()
	{
		if (_dbmsStartTime == null)
			return -1;
		
		long msDiff = System.currentTimeMillis() - _dbmsStartTime.getTime();

		//return (int) (msDiff / (1000*60*60*24));
		return (int) TimeUnit.MILLISECONDS.toDays(msDiff);
	}


	/** Get the DbxTune application type that recorded this info */
	public String getRecDbxAppName()
	{
//		return _recAppName;
		return _monVersionInfo._productString;
	}

	public String getRecDbxVersionStr()
	{
//		return _recVersionString;
		return _monVersionInfo._versionString;
	}

	/** Get the DbxTune application type that recorded this info */
	public String getRecDbxBuildStr()
	{
//		return _recBuildString;
		return _monVersionInfo._buildString;
	}

	/** Get the DbxTune full MonVersionInfo */
	public MonVersionInfo getMonVersionInfo()
	{
		return _monVersionInfo;
	}

	/** Initialize members  
	 * @param sessionStartTime */
	private void initialize(DbxConnection conn, Timestamp sessionStartTime)
	{
		// Get DbxTune application name from the recorded session database
//		String appName = "";
//		if (true) // get from database...
//		{
//			String sql = "select max([ProductString]), max([VersionString]), max([BuildString]) from [MonVersionInfo]";
//
//			sql = conn.quotifySqlString(sql);
//			try ( Statement stmnt = conn.createStatement() )
//			{
//				// Unlimited execution time
//				stmnt.setQueryTimeout(0);
//				try ( ResultSet rs = stmnt.executeQuery(sql) )
//				{
//					while(rs.next())
//					{
//						appName           = rs.getString(1);
//						_recVersionString = rs.getString(2);
//						_recBuildString   = rs.getString(3);
//					}
//				}
//			}
//			catch(SQLException ex)
//			{
//				_logger.warn("Problems getting DbxTune Collector string using SQL '"+sql+"'. Caught: " + ex);
//			}
//		}
//		if (StringUtil.isNullOrBlank(appName))
//		{
//			appName = Version.getAppName();
//		}
		
		// Get info about "WHO" made the recording
		_monVersionInfo = PersistReader.getMonVersionInfo(conn, sessionStartTime);
		String appName = _monVersionInfo._productString;


		String dbmsVersionColName   = "srvVersion";
		String dbmsSrvNameColName   = "serverName";
		String dbmsStartDateColName = "StartDate";
		String tabName = "CmSummary_abs";

		// What table/column is the "server version" stored in
		if      ("AseTune"      .equalsIgnoreCase(appName)) { dbmsVersionColName = "srvVersion";  dbmsSrvNameColName = "atAtServerName"; dbmsStartDateColName = "StartDate";     tabName = "CmSummary_abs"; }
		else if ("IqTune"       .equalsIgnoreCase(appName)) { dbmsVersionColName = "atAtVersion"; dbmsSrvNameColName = "atAtServerName"; dbmsStartDateColName = null;            tabName = "CmSummary_abs"; }
		else if ("RsTune"       .equalsIgnoreCase(appName)) { dbmsVersionColName = "rsVersion";   dbmsSrvNameColName = "serverName";     dbmsStartDateColName = null;            tabName = "CmSummary_abs"; }
		else if ("RaxTune"      .equalsIgnoreCase(appName)) { dbmsVersionColName = "NOT_STORED";  dbmsSrvNameColName = "atAtServerName"; dbmsStartDateColName = null;            tabName = "CmSummary_abs"; }
		else if ("SqlServerTune".equalsIgnoreCase(appName)) { dbmsVersionColName = "srvVersion";  dbmsSrvNameColName = "atAtServerName"; dbmsStartDateColName = "StartDate";     tabName = "CmSummary_abs"; }
		else if ("PostgresTune" .equalsIgnoreCase(appName)) { dbmsVersionColName = "version";     dbmsSrvNameColName = "instance_name";  dbmsStartDateColName = "start_time";    tabName = "CmSummary_abs"; }
		else if ("MySqlTune"    .equalsIgnoreCase(appName)) { dbmsVersionColName = "version";     dbmsSrvNameColName = "host";           dbmsStartDateColName = "start_time";    tabName = "CmSummary_abs"; }
		else if ("OracleTune"   .equalsIgnoreCase(appName)) { dbmsVersionColName = "VERSION";     dbmsSrvNameColName = "INSTANCE_NAME";  dbmsStartDateColName = "STARTUP_TIME";  tabName = "CmSummary_abs"; }
		else if ("Db2Tune"      .equalsIgnoreCase(appName)) { dbmsVersionColName = "VERSION";     dbmsSrvNameColName = "DATABASE_NAME";  dbmsStartDateColName = null;            tabName = "CmSummary_abs"; }
		else if ("HanaTune"     .equalsIgnoreCase(appName)) { dbmsVersionColName = "VERSION";     dbmsSrvNameColName = "DATABASE_NAME";  dbmsStartDateColName = null;            tabName = "CmSummary_abs"; }

		String sqlVersion = "'' as [Version]";
		if (StringUtil.hasValue(dbmsVersionColName))
			sqlVersion = "max([" + dbmsVersionColName + "])";
		
		String sqlSrvName = "'' as [SrvName]";
		if (StringUtil.hasValue(dbmsSrvNameColName))
			sqlSrvName = "max([" + dbmsSrvNameColName + "])";
		
		String sqlStartDate = "NULL as [StartDate]";
		if (StringUtil.hasValue(dbmsStartDateColName))
			sqlStartDate = "max([" + dbmsStartDateColName + "])";
		
		// Construct SQL and get the version string
		String sql = "select " + sqlVersion + ", " + sqlSrvName + ", " + sqlStartDate 
				+ " from [" + tabName + "]";
		
		String    dbmsVersionString = "";
		String    dbmsSrvName       = "";
		Timestamp dbmsStartTime     = null;
		
		sql = conn.quotifySqlString(sql);
		try ( Statement stmnt = conn.createStatement() )
		{
			// Unlimited execution time
			stmnt.setQueryTimeout(0);
			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				while(rs.next())
				{
					dbmsVersionString = rs.getString   (1);
					dbmsSrvName       = rs.getString   (2);
					dbmsStartTime     = rs.getTimestamp(3);
				}
			}
		}
		catch(SQLException ex)
		{
			_logger.warn("Problems getting version string using SQL '"+sql+"'. Caught: " + ex);
		}

		
		// Parse the Version String into a number
		long ver = 0; 

		if      ("AseTune"      .equalsIgnoreCase(appName)) { ver = Ver.sybVersionStringToNumber      (dbmsVersionString); }
		else if ("IqTune"       .equalsIgnoreCase(appName)) { ver = Ver.iqVersionStringToNumber       (dbmsVersionString); }
		else if ("RsTune"       .equalsIgnoreCase(appName)) { ver = Ver.sybVersionStringToNumber      (dbmsVersionString); }
		else if ("RaxTune"      .equalsIgnoreCase(appName)) { ver = Ver.sybVersionStringToNumber      (dbmsVersionString); }
		else if ("SqlServerTune".equalsIgnoreCase(appName)) { ver = Ver.sqlServerVersionStringToNumber(dbmsVersionString); }
		else if ("PostgresTune" .equalsIgnoreCase(appName)) { ver = Ver.shortVersionStringToNumber(VersionShort.parse(dbmsVersionString)); }
		else if ("MySqlTune"    .equalsIgnoreCase(appName)) { ver = Ver.shortVersionStringToNumber(VersionShort.parse(dbmsVersionString)); }
		else if ("OracleTune"   .equalsIgnoreCase(appName)) { ver = Ver.oracleVersionStringToNumber   (dbmsVersionString); }
		else if ("Db2Tune"      .equalsIgnoreCase(appName)) { ver = Ver.db2VersionStringToNumber      (dbmsVersionString); }
		else if ("HanaTune"     .equalsIgnoreCase(appName)) { ver = Ver.hanaVersionStringToNumber     (dbmsVersionString); }

		_sessionStartTime  = _monVersionInfo._sessionStartTime;
//		_recAppName        = appName;
		_dbmsVersionString = dbmsVersionString;
		_dbmsServerName    = dbmsSrvName;
		_dbmsVersionNum    = ver;
		_dbmsStartTime     = dbmsStartTime;
	}
}
