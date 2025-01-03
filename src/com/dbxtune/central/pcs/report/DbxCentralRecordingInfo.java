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
package com.dbxtune.central.pcs.report;

import java.io.IOException;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.Version;
import com.dbxtune.central.lmetrics.LocalMetricsCollectorThread;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.ReportEntryAbstract;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.NumberUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;

public class DbxCentralRecordingInfo
extends ReportEntryAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	// Local Metrics Recordings
	private Timestamp _lmr_startTimeTs = null;
	private Timestamp _lmr_endTimeTs   = null;
	private String    _lmr_startTime   = null; 
	private String    _lmr_endTime     = null;
	private String    _lmr_duration    = null;
	private String    _lmr_startDay    = null;
	private int       _lmr_recordingSampleTimeConf = -1;
	private double    _lmr_recordingSampleTimeAct  = -1;
	

	// All Servers Recordings
	private Timestamp _asr_startTimeTs = null;
	private Timestamp _asr_endTimeTs   = null;
	private String    _asr_startTime   = null;
	private String    _asr_endTime     = null;
	private String    _asr_duration    = null;
	private String    _asr_startDay    = null;

	@SuppressWarnings("unused")
	private Exception _problem     = null;

//	private String _dbmsVersionString;
//	private String _dbmsServerName;
//	private String _dbmsDisplayName;
//	private String _dbmsStartTimeStr;
//	private String _dbmsStartTimeInDaysStr;

	private Map<String, String> _dbmsOtherInfoMap = new LinkedHashMap<>();
	
	private String _reportVersion    = Version.getAppName() + ", Version: " + Version.getVersionStr() + ", Build: " + Version.getBuildStr();
//	private String _recordingVersion = null;
	
	private String _osCoreInfo   = "";
	private String _osMemoryInfo = "";

//	private boolean _lmr_isHostMonitoringEnabled = false;
	private String  _lmr_hostMonitorHostname;

	public DbxCentralRecordingInfo(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	public Timestamp getLmrStartTimeTs()         { return _lmr_startTimeTs; } 
	public Timestamp getLmrEndTimeTs()           { return _lmr_endTimeTs;   } 
	public String    getLmrStartTime()           { return _lmr_startTime;   } 
	public String    getLmrEndTime()             { return _lmr_endTime;     } 
	public String    getLmrDuration()            { return _lmr_duration;    } 
	public String    getLmrStartDay()            { return _lmr_startDay;    } 
//	public String    getDbmsVersionString()   { return _dbmsVersionString; } 
//	public String    getDbmsServerName()      { return _dbmsServerName; } 
//	public String    getDbmsDisplayName()     { return _dbmsDisplayName; } 

	public Timestamp getAsrStartTimeTs()         { return _asr_startTimeTs; } 
	public Timestamp getAsrEndTimeTs()           { return _asr_endTimeTs;   } 
	public String    getAsrStartTime()           { return _asr_startTime;   } 
	public String    getAsrEndTime()             { return _asr_endTime;     } 
	public String    getAsrDuration()            { return _asr_duration;    } 
	public String    getAsrStartDay()            { return _asr_startDay;    } 
	
	@Override
	public boolean hasMinimalMessageText()
	{
		return true;
	}

	@Override
	public boolean hasShortMessageText()
	{
		return true;
	}

//	@Override
//	public void writeShortMessageText(Writer w)
//	throws IOException
//	{
//		writeMessageText(w);
//	}

	@Override
	public void writeMessageText(Writer sb, MessageType messageType)
	throws IOException
	{
		String blankTableRow = "  <tr> <td>&nbsp;</td> <td>&nbsp;</td> <td>&nbsp;</td> </tr>\n";
		String tdBullet      = "<td>&emsp;&bull;&nbsp;</td>";    // Bull
//		String tdBullet      = "<td>&emsp;&rArr;&nbsp;</td>";    // rightwards double arrow
//		String tdBullet      = "<td>&emsp;&#9679;&nbsp;</td>";   // BLACK CIRCLE
//		String tdBullet      = "<td>&emsp;&#10063;&nbsp;</td>";  // LOWER RIGHT DROP-SHADOWED WHITE SQUARE
		
		if (_lmr_startTime == null)
		{
			sb.append("Can't get start/end time <br>\n");
		}
		else
		{
			sb.append("<style type='text/css'>         \n");
			sb.append("    table.recording-info td     \n");
			sb.append("    {                           \n");
			sb.append("         border-width: 0px;     \n");
			sb.append("         padding: 1px;          \n");
			sb.append("    }                           \n");
			sb.append("    table.recording-info tr:nth-child(even) \n");
			sb.append("    {                           \n");
//			sb.append("         background-color: white !important; \n");
			sb.append("         background-color: transparent; \n");
			sb.append("    }                           \n");
			sb.append("</style> \n");

			sb.append("<br>\n");

			sb.append("<table class='recording-info'>\n");

			sb.append("  <tr> " + tdBullet +" <td><b>The Report is Produced by : </b></td> <td>" + _reportVersion         + "</td> </tr>\n");
//			sb.append("  <tr> " + tdBullet +" <td><b>Recording was Made Using:   </b></td> <td>" + _recordingVersion      + "</td> </tr>\n");

			if (getReportingInstance().hasReportPeriod())
			{
				sb.append(blankTableRow);
				sb.append("  <tr> " + tdBullet +" <td><b>Report Period Begin Day:  </b></td> <td>" + getReportingInstance().getReportPeriodBeginTime().toLocalDateTime().getDayOfWeek().name() + "</td> </tr>\n");
				sb.append("  <tr> " + tdBullet +" <td><b>Report Period Begin Time: </b></td> <td>" + TimeUtils.toStringYmdHms(getReportingInstance().getReportPeriodBeginTime()) + "</td> </tr>\n");
				sb.append("  <tr> " + tdBullet +" <td><b>Report Period End Time    </b></td> <td>" + TimeUtils.toStringYmdHms(getReportingInstance().getReportPeriodEndTime()  ) + "</td> </tr>\n");
				sb.append("  <tr> " + tdBullet +" <td><b>Report Period Duration:   </b></td> <td>" + getReportingInstance().getReportPeriodDuration()  + "</td> </tr>\n");
			}
			else
			{
				sb.append(blankTableRow);
				sb.append("  <tr> " + tdBullet +" <td><b>Report Period: </b></td> <td> <b>Full</b> period</td> </tr>\n");
			}
			sb.append(blankTableRow);
//			sb.append("  <tr> " + tdBullet +" <td><b>Local Metric Recording Start Day:          </b></td> <td>" + _lmr_startDay                + "</td> </tr>\n");
			sb.append("  <tr> " + tdBullet +" <td><b>Local Metric Recording Start Date:         </b></td> <td>" + _lmr_startTime               + "</td> </tr>\n");
			sb.append("  <tr> " + tdBullet +" <td><b>Local Metric Recording End  Date:          </b></td> <td>" + _lmr_endTime                 + "</td> </tr>\n");
			sb.append("  <tr> " + tdBullet +" <td><b>Local Metric Recording Duration:           </b></td> <td>" + _lmr_duration                + "</td> </tr>\n");
			sb.append("  <tr> " + tdBullet +" <td><b>Local Metric Recording Conf Sample Time:   </b></td> <td>" + _lmr_recordingSampleTimeConf + "</td> </tr>\n");
			sb.append("  <tr> " + tdBullet +" <td><b>Local Metric Recording Actual Sample Time: </b></td> <td>" + _lmr_recordingSampleTimeAct  + "</td> </tr>\n");
			sb.append("  <tr> " + tdBullet +" <td><b>Local Metric Recording Hostname:           </b></td> <td>" + _lmr_hostMonitorHostname     + "</td> </tr>\n");

			sb.append(blankTableRow);
//			sb.append("  <tr> " + tdBullet +" <td><b>All Servers Recording Start Day:           </b></td> <td>" + _asr_startDay              + "</td> </tr>\n");
			sb.append("  <tr> " + tdBullet +" <td><b>All Servers Recording Start Date:          </b></td> <td>" + _asr_startTime             + "</td> </tr>\n");
			sb.append("  <tr> " + tdBullet +" <td><b>All Servers Recording End  Date:           </b></td> <td>" + _asr_endTime               + "</td> </tr>\n");
			sb.append("  <tr> " + tdBullet +" <td><b>All Servers Recording Duration:            </b></td> <td>" + _asr_duration              + "</td> </tr>\n");
//			sb.append("  <tr> " + tdBullet +" <td><b>All Servers Recording Sample Time:         </b></td> <td>" + _asr_recordingSampleTime   + "</td> </tr>\n");

			// Central Recording DBMS Info
			sb.append(blankTableRow);
			if (_dbmsOtherInfoMap != null && !_dbmsOtherInfoMap.isEmpty())
			{
				sb.append(blankTableRow);
				for (Entry<String, String> entry : _dbmsOtherInfoMap.entrySet())
				{
		   			sb.append("  <tr> " + tdBullet +" <td><b>" + entry.getKey() + ":  </b></td> <td>" + entry.getValue()   + "</td> </tr>\n");
				}
			}

			// OS info: CORE Count, MEMORY ...
			sb.append(blankTableRow);
			sb.append("  <tr> " + tdBullet +" <td><b>OS Core Count:                </b></td> <td>" + _osCoreInfo   + "</td> </tr>\n");
			sb.append("  <tr> " + tdBullet +" <td><b>OS Physical Memory:           </b></td> <td>" + _osMemoryInfo + "</td> </tr>\n");

			// Java Version Info
			sb.append(blankTableRow);
			sb.append("  <tr> " + tdBullet +" <td><b>Java Version:                 </b></td> <td>" + System.getProperty("java.version")      + "</td> </tr>\n");
			sb.append("  <tr> " + tdBullet +" <td><b>Java Vendor:                  </b></td> <td>" + System.getProperty("java.vendor")       + "</td> </tr>\n");
			sb.append("  <tr> " + tdBullet +" <td><b>Java Home:                    </b></td> <td>" + System.getProperty("java.home")         + "</td> </tr>\n");
			sb.append("  <tr> " + tdBullet +" <td><b>Java Version Date:            </b></td> <td>" + System.getProperty("java.version.date") + "</td> </tr>\n");

			sb.append("</table>\n");
		}
	}

	@Override
	public boolean canBeDisabled()
	{
		return false;
	}

	@Override
	public String getSubject()
	{
		return "Recording Information";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false;
	}

	@Override
	public String[] getMandatoryTables()
	{
//		return new String[] { PersistWriterBase.getTableName(null, PersistWriterBase.SESSION_SAMPLES, null, false) };
//		return new String[] { "DbxCentralVersionInfo" };
//		return new String[] { 
//				CentralPersistWriterBase.getTableName(null, 
//						getReportingInstance().getDbmsSchemaName(), 
//						CentralPersistWriterBase.Table.CENTRAL_VERSION_INFO, 
//						null, false) };
		return new String[] { "CmOsMpstat_abs" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		String sql;

//		if (hasReportingInstance())
//		{
//			DailySummaryReportAbstract dsr = getReportingInstance();
//			_dbmsVersionString      = dsr.getDbmsVersionStr();
//			_dbmsServerName         = dsr.getDbmsServerName();
//			_dbmsStartTimeStr       = dsr.getDbmsStartTime()       == null ? "-unknown-" : dsr.getDbmsStartTime().toString();
//			_dbmsStartTimeInDaysStr = dsr.getDbmsStartTimeInDays()     < 0 ? "-unknown-" : dsr.getDbmsStartTimeInDays()+"";
//			_dbmsOtherInfoMap       = dsr.getDbmsOtherInfoMap();
//			_recordingVersion       = dsr.getRecDbxAppName() + ", Version: " + dsr.getRecDbxVersionStr() + ", Build: " + dsr.getRecDbxBuildStr();
//
//			if (dsr.getReportContent() != null)
//				_dbmsDisplayName = dsr.getReportContent().getDisplayOrServerName();
//		}

		//-----------------------------------------
		// Get "sample time" for the recording
		//-----------------------------------------
//		_recordingSampleTime = StringUtil.parseInt(getRecordingSessionParameter(conn, null, "offline.sampleTime"), -1);
		//_recordingSampleTime = getRecordingSampleTime(conn);

		//-----------------------------------------
		// Get Host Monitoring HOSTNAME
		//-----------------------------------------
//		_hostMonitorHostname = getRecordingSessionParameter(conn, null, "conn.sshHostname");

		//-----------------------------------------
		// Get/Check if Host Monitoring was enabled
		//-----------------------------------------
//		_isHostMonitoringEnabled = isHostMonitoringEnabled(conn);

		//-----------------------------------------
		// OS CoreCound & MemoryInfo
		//-----------------------------------------
		_osCoreInfo   = getOsCoreInfo(conn);
		_osMemoryInfo = getOsMemoryInfo(conn);

		//-----------------------------------------
		// Add Some information about the Central DBMS
		//-----------------------------------------
		try
		{
			_dbmsOtherInfoMap.put("Central DBMS Vendor" , conn.getMetaData().getDatabaseProductName());
			_dbmsOtherInfoMap.put("Central DBMS Version", conn.getMetaData().getDatabaseProductVersion());
		}
		catch(SQLException ex)
		{
			_problem = ex;

			_logger.warn("Problems getting: '" + getSubject() + "', Caught: " + ex);
		}
		
		
		//-----------------------------------------
		// Local Metrics Recordings --- Start/end time
		//-----------------------------------------
//		String tableName        = CentralPersistWriterBase.getTableName(null, getReportingInstance().getDbmsSchemaName(), CentralPersistWriterBase.Table.CENTRAL_SESSIONS,	null, false);
//		String schemaNamePrefix = "";//getReportingInstance().getDbmsSchemaNameSqlPrefix();
		String tableName        = "CmOsMpstat_abs";
		String schemaNamePrefix = getReportingInstance().getDbmsSchemaNameSqlPrefix();
		
		// NOTE: Or should we look at the *whole* DbxCentral recording (and not "just" the Local-Metrics)

		sql = "select min([SessionSampleTime]), max([SessionSampleTime]), count(*) \n" +
		      "from " + schemaNamePrefix + "[" + tableName + "] \n";

		sql = conn.quotifySqlString(sql);
		try ( Statement stmnt = conn.createStatement() )
		{
			// Unlimited execution time
			stmnt.setQueryTimeout(0);
			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				while (rs.next())
				{
					Timestamp startTime = rs.getTimestamp(1);
					Timestamp endTime   = rs.getTimestamp(2);
					int       count     = rs.getInt      (3);
					
					_lmr_startTimeTs = startTime;
					_lmr_endTimeTs   = endTime;

					if (startTime != null && endTime != null)
					{
						_lmr_startTime = TimeUtils.toStringYmdHms(startTime);
						_lmr_endTime   = TimeUtils.toStringYmdHms(endTime);
						_lmr_duration  = TimeUtils.msToTimeStrDHMS(endTime.getTime() - startTime.getTime() );
						_lmr_startDay  = startTime.toLocalDateTime().getDayOfWeek().name();

						_lmr_duration  = TimeUtils.msToTimeStrDHMS( endTime.getTime() - startTime.getTime() ) + "   ([#d] HH:MM:SS)";

						long sampleTimeSec = (endTime.getTime() - startTime.getTime()) / 1000;
						_lmr_recordingSampleTimeAct = NumberUtils.round( sampleTimeSec*1.0 / count*1.0, 1);
					}
				}
			}
			_lmr_recordingSampleTimeConf = Configuration.getCombinedConfiguration().getIntProperty(LocalMetricsCollectorThread.PROPKEY_sampleTime, LocalMetricsCollectorThread.DEFAULT_sampleTime);
		}
		catch(SQLException ex)
		{
			_problem = ex;

			_logger.warn("Problems getting: '" + getSubject() + "', Caught: " + ex);
		}
		
		try 
		{ 
//			_lmr_hostMonitorHostname = InetAddress.getLocalHost().getHostName();
			_lmr_hostMonitorHostname = InetAddress.getLocalHost().getCanonicalHostName();
		}
		catch (UnknownHostException ex)
		{
			_logger.warn("Problems getting hostname for DbxTune Central. Caught: " + ex);
		}

	
		//-----------------------------------------
		// All Servers Recordings --- Start/end time
		//-----------------------------------------
		schemaNamePrefix = ""; // The "public" schema
		tableName        = "DbxCentralSessions";

		sql = "select min([SessionStartTime]), max([LastSampleTime]) \n" +
			      "from " + schemaNamePrefix + "[" + tableName + "] \n";

		sql = conn.quotifySqlString(sql);
		try ( Statement stmnt = conn.createStatement() )
		{
			// Unlimited execution time
			stmnt.setQueryTimeout(0);
			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				while (rs.next())
				{
					Timestamp startTime = rs.getTimestamp(1);
					Timestamp endTime   = rs.getTimestamp(2);
					
					_asr_startTimeTs = startTime;
					_asr_endTimeTs   = endTime;

					if (startTime != null && endTime != null)
					{
						
						_asr_startTime = TimeUtils.toStringYmdHms(startTime);
						_asr_endTime   = TimeUtils.toStringYmdHms(endTime);
						_asr_duration  = TimeUtils.msToTimeStrDHMS(endTime.getTime() - startTime.getTime() );
						_asr_startDay  = startTime.toLocalDateTime().getDayOfWeek().name();

						_asr_duration  = TimeUtils.msToTimeStrDHMS( endTime.getTime() - startTime.getTime() ) + "   ([#d] HH:MM:SS)";
					}
				}
			}
		}
		catch(SQLException ex)
		{
			_problem = ex;

			_logger.warn("Problems getting: '" + getSubject() + "', Caught: " + ex);
		}
	}

//	private boolean isHostMonitoringEnabled(DbxConnection conn)
//	{
//		// Check if any of the tables exists... exit as soon as you find a table
//		if (doTableExist(conn, null, "CmOsDiskSpace_abs")) return true;
//		if (doTableExist(conn, null, "CmOsIostat_abs"   )) return true;
//		if (doTableExist(conn, null, "CmOsMeminfo_abs"  )) return true;
//		if (doTableExist(conn, null, "CmOsMpstat_abs"   )) return true;
//		if (doTableExist(conn, null, "CmOsNwInfo_abs"   )) return true;
//		if (doTableExist(conn, null, "CmOsUptime_abs"   )) return true;
//		if (doTableExist(conn, null, "CmOsVmstat_abs"   )) return true;
//		
//		return false;
//	}

//	private String getOsMemoryInfo(DbxConnection conn)
//	{
//		if (isWindows())
//		{
//			return "Physical Memory on Windows is not supported right now, sorry.";
//		}
//
//		if ( ! doTableExist(conn, null, "CmOsMeminfo_abs"  ) )
//		{
//			_logger.info("getOsMemoryInfo(): No PCS Table 'CmOsMeminfo_abs' was found.");
//			return "No PCS Table 'CmOsMeminfo_abs' was found.";
//		}
//		
//		String schemaNamePrefix = getReportingInstance().getDbmsSchemaNameSqlPrefix();
//
//		int memTotalMb = -1;
//		String sql = ""
//			    + "select [used] / 1024 as [MemTotalMb] \n"
//			    + "from " + schemaNamePrefix + "[CmOsMeminfo_abs] \n"
//			    + "where [memoryType] = 'MemTotal' \n"
//			    + "  and [SessionSampleTime] = (select max([SessionSampleTime]) from " + schemaNamePrefix + "[CmOsMeminfo_abs]) \n"
//			    + "";
//
//		sql = conn.quotifySqlString(sql);
//		try ( Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
//		{
//			while (rs.next())
//			{
//				memTotalMb = rs.getInt(1);
//			}
//		}
//		catch(SQLException ex)
//		{
//			_logger.warn("Problems getting: 'getOsMemoryInfo()', Caught: " + ex);
//			memTotalMb = -1;
//		}
//		
//		TODO; // set ### GB, ### MB
//		if (memTotalMb > 0)
//			return "" + NumberUtils.round((memTotalMb / 1024.0), 1); // to GB
//		else
//			return "";
//	}
//
//	private String getOsCoreInfo(DbxConnection conn)
//	{
//		if ( ! doTableExist(conn, null, "CmOsMpstat_abs"  ) )
//		{
//			_logger.info("getOsCoreInfo(): No PCS Table 'CmOsMpstat_abs' was found.");
//			return "No PCS Table 'CmOsMpstat_abs' was found.";
//		}
//
//		String schemaNamePrefix = getReportingInstance().getDbmsSchemaNameSqlPrefix();
//
//		int coreCount = -1;
//		String sql = "";
//		
//		// LINUX
//		sql = ""
//			+ "select count(*) as [CoreCount] \n"
//			+ "from " + schemaNamePrefix + "[CmOsMpstat_abs] \n"
//			+ "where [CPU] != 'all'  \n"
//			+ "  and [SessionSampleTime] = (select max([SessionSampleTime]) from " + schemaNamePrefix + "[CmOsMpstat_abs])";
//		
//		// Windows
//		if (isWindows())
//		{
//			sql = "select count(*) as [CoreCount] \n"
//			    + "from " + schemaNamePrefix + "[CmOsMpstat_abs] \n"
//			    + "where [Instance] != '_Total' \n"
//			    + "  and [SessionSampleTime] = (select max([SessionSampleTime]) from " + schemaNamePrefix + "[CmOsMpstat_abs]) \n"
//			    + "";
//		}
//
//		sql = conn.quotifySqlString(sql);
//		try ( Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
//		{
//			while (rs.next())
//			{
//				coreCount = rs.getInt(1);
//			}
//		}
//		catch(SQLException ex)
//		{
//			_logger.warn("Problems getting: 'getOsCoreInfo()', Caught: " + ex);
//			coreCount = -1;
//		}
//		
//		if (coreCount > 0)
//			return "" + coreCount;
//		else
//			return "";
//	}
	
	/**
	 * Get OS Memory Information (and SQL Server specifics, is available)
	 * @param conn A Connection the the PCS Storage
	 * @return A String with various information
	 */
	private String getOsMemoryInfo(DbxConnection conn)
	{
		// Get SQL Server info
		String sqlServerInfo = "";
//		if (StringUtil.hasValue(_dbmsVersionString) && _dbmsVersionString.contains("SQL Server")) 
//		{
//			Map<String, String> sysInfo = getSqlServerConfig_SysInfo(conn);
//			if (sysInfo != null && !sysInfo.isEmpty())
//			{
//				long   physical_memory_kb = StringUtil.parseLong(sysInfo.get("physical_memory_kb"), 0);
//				double physical_memory_mb = NumberUtils.round((physical_memory_kb / 1024.0)         , 1);
//				double physical_memory_gb = NumberUtils.round((physical_memory_kb / 1024.0 / 1024.0), 1);
//
//				sqlServerInfo = "dm_os_sys_info: physical_memory_GB=" + physical_memory_gb + ", physical_memory_MB=" + physical_memory_mb;
//			}
//		}

		// NOT SQL-Server and on Windows ... NOT Supported
		if (isWindows() && StringUtil.isNullOrBlank(sqlServerInfo))
		{
			return "Physical Memory on Windows is not supported right now, sorry.";
			// We could "possibly" get it from "CmOsMeminfo_abs" but I couldn't work out the columns to use...
			// I tried: [Available Bytes], [Committed Bytes] and [Commit Limit] but couldn't work out the details... 
			//     and ([Available Bytes] + [Committed Bytes]) / 1024.0 / 1024.0  looks like the "closest"
			// So lets do that at a later stage... 
		}

		// On Linux Only -- Get OS info
		String linuxInfo = "";
		if ( ! isWindows() )
		{
			if ( ! doTableExist(conn, null, "CmOsMeminfo_abs"  ) )
			{
				_logger.info("getOsMemoryInfo(): No PCS Table 'CmOsMeminfo_abs' was found.");
				linuxInfo = "No PCS Table 'CmOsMeminfo_abs' was found.";
			}
			else
			{
				String schemaNamePrefix = getReportingInstance().getDbmsSchemaNameSqlPrefix();

				long memTotalKb = -1;
				String sql = ""
					    + "select [used] as [MemTotalKb] \n"
					    + "from " + schemaNamePrefix + "[CmOsMeminfo_abs] \n"
					    + "where [memoryType] = 'MemTotal' \n"
					    + "  and [SessionSampleTime] = (select max([SessionSampleTime]) from " + schemaNamePrefix + "[CmOsMeminfo_abs]) \n"
					    + "";

				sql = conn.quotifySqlString(sql);
				try ( Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
				{
					while (rs.next())
					{
						memTotalKb = rs.getLong(1);
					}
				}
				catch(SQLException ex)
				{
					_logger.warn("Problems getting: 'getOsMemoryInfo()', Caught: " + ex);
					memTotalKb = -1;
				}
				
				if (memTotalKb > 0)
				{
					double memTotalMb = NumberUtils.round((memTotalKb / 1024.0)         , 1);
					double memTotalGb = NumberUtils.round((memTotalKb / 1024.0 / 1024.0), 1);
					linuxInfo = memTotalMb + " MB, " + memTotalGb + " GB";
				}
			}
		}

		// Decide what to output
		String output = "";

		if (StringUtil.hasValue(linuxInfo))
			output += "OsInfo: " + linuxInfo;

		if (StringUtil.hasValue(sqlServerInfo))
		{
			if (StringUtil.hasValue(output))
				output += "; ";

			output += "SqlServerInfo: " + sqlServerInfo;
		}
		
		return output;
	}

	/**
	 * Get OS Core/CPU Information (and SQL Server specifics, is available)
	 * @param conn A Connection the the PCS Storage
	 * @return A String with various information
	 */
	private String getOsCoreInfo(DbxConnection conn)
	{
		// Get SQL Server info
		String sqlServerInfo = "";
//		if (StringUtil.hasValue(_dbmsVersionString) && _dbmsVersionString.contains("SQL Server"))
//		{
//			Map<String, String> sysInfo = getSqlServerConfig_SysInfo(conn);
//			if (sysInfo != null && !sysInfo.isEmpty())
//			{
//				String cpu_count                 = "cpu_count="                 + sysInfo.get("cpu_count");
//				String hyperthread_ratio         = "hyperthread_ratio="         + sysInfo.get("hyperthread_ratio");
//				String socket_count              = "socket_count="              + sysInfo.get("socket_count");
//				String cores_per_socket          = "cores_per_socket="          + sysInfo.get("cores_per_socket");
//				String numa_node_count           = "numa_node_count="           + sysInfo.get("numa_node_count");
//				String virtual_machine_type_desc = "virtual_machine_type_desc=" + sysInfo.get("virtual_machine_type_desc");
//				
//				sqlServerInfo = "dm_os_sys_info: " + StringUtil.toCommaStr(cpu_count, hyperthread_ratio, socket_count, cores_per_socket, numa_node_count, virtual_machine_type_desc);
//			}
//		}

		// Get OS info
		String osInfo = "";
		if ( ! doTableExist(conn, null, "CmOsMpstat_abs"  ) )
		{
			_logger.info("getOsCoreInfo(): No PCS Table 'CmOsMpstat_abs' was found.");
			osInfo = "No PCS Table 'CmOsMpstat_abs' was found.";
		}
		else
		{
			String schemaNamePrefix = getReportingInstance().getDbmsSchemaNameSqlPrefix();

			int coreCount = -1;
			String sql = "";
			
			// LINUX
			sql = ""
				+ "select count(*) as [CoreCount] \n"
				+ "from " + schemaNamePrefix + "[CmOsMpstat_abs] \n"
				+ "where [CPU] != 'all'  \n"
				+ "  and [SessionSampleTime] = (select max([SessionSampleTime]) from " + schemaNamePrefix + "[CmOsMpstat_abs])";
			
			// Windows
			if (isWindows())
			{
				sql = "select count(*) as [CoreCount] \n"
				    + "from " + schemaNamePrefix + "[CmOsMpstat_abs] \n"
				    + "where [Instance] != '_Total' \n"
				    + "  and [SessionSampleTime] = (select max([SessionSampleTime]) from " + schemaNamePrefix + "[CmOsMpstat_abs]) \n"
				    + "";
			}

			sql = conn.quotifySqlString(sql);
			try ( Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while (rs.next())
				{
					coreCount = rs.getInt(1);
				}
			}
			catch(SQLException ex)
			{
				_logger.warn("Problems getting: 'getOsCoreInfo()', Caught: " + ex);
				coreCount = -1;
			}
			
			if (coreCount > 0)
				osInfo = "coreCount=" + coreCount;
		}


		// Decide what to output
		String output = "";

		if (StringUtil.hasValue(osInfo))
			output += "OsInfo: " + osInfo;

		if (StringUtil.hasValue(sqlServerInfo))
		{
			if (StringUtil.hasValue(output))
				output += "; ";

			output += "SqlServerInfo: " + sqlServerInfo;
		}
		
		return output;
	}
}
