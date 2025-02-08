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
package com.dbxtune.pcs.report.content;

import java.io.IOException;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
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
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.PersistWriterBase;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.NumberUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;

public class RecordingInfo
extends ReportEntryAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private Timestamp _startTimeTs = null;
	private Timestamp _endTimeTs   = null;
	private String    _startTime   = null;
	private String    _endTime     = null;
	private String    _duration    = null;
	private String    _startDay    = null;

	private Exception _problem     = null;

	private String    _dbmsVersionString;
	private String    _dbmsVersionStringMin;
	private String    _dbmsVersionStringMax;
	private Timestamp _dbmsVersionStringChangeTime;
	private String    _dbmsServerName;
	private String    _dbmsDisplayName;
	private String    _dbmsStartTimeStr;
	private String    _dbmsStartTimeInDaysStr;

	private Map<String, String> _dbmsOtherInfoMap;
	
	private String _reportVersion    = Version.getAppName() + ", Version: " + Version.getVersionStr() + ", Build: " + Version.getBuildStr();
	private String _recordingVersion = null;
	
	private int    _recordingSampleTime = -1;

	private String _osCoreInfo   = "";
	private String _osMemoryInfo = "";

	private boolean _isHostMonitoringEnabled = false;
	private String  _hostMonitorHostname;

	public RecordingInfo(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	public Timestamp getStartTimeTs()         { return _startTimeTs; } 
	public Timestamp getEndTimeTs()           { return _endTimeTs; } 
	public String    getStartTime()           { return _startTime; } 
	public String    getEndTime()             { return _endTime; } 
	public String    getDuration()            { return _duration; } 
	public String    getStartDay()            { return _startDay; } 
	public String    getDbmsVersionString()   { return _dbmsVersionString; } 
	public String    getDbmsServerName()      { return _dbmsServerName; } 
	public String    getDbmsDisplayName()     { return _dbmsDisplayName; } 
	
	public String    getOsCoreInfo()             { return _osCoreInfo; }
	public String    getOsMemoryInfo()           { return _osMemoryInfo; }
	public String    getHostMonitorHostname()    { return _hostMonitorHostname; }
	public boolean   isHostMonitoringEnabled()   { return _isHostMonitoringEnabled; }

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
		
		if (_startTime == null)
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

			sb.append("  <tr> " + tdBullet +" <td><b>Recording was Made Using:   </b></td> <td>" + _recordingVersion      + "</td> </tr>\n");
			sb.append("  <tr> " + tdBullet +" <td><b>The Report is Produced by : </b></td> <td>" + _reportVersion         + "</td> </tr>\n");
			if (getReportingInstance().hasReportPeriod())
			{
				sb.append(blankTableRow);
				sb.append("  <tr> " + tdBullet +" <td><b>Report Period Begin Time: </b></td> <td>" + getReportingInstance().getReportPeriodBeginTime() + "</td> </tr>\n");
				sb.append("  <tr> " + tdBullet +" <td><b>Report Period End Time    </b></td> <td>" + getReportingInstance().getReportPeriodEndTime()   + "</td> </tr>\n");
				sb.append("  <tr> " + tdBullet +" <td><b>Report Period Duration:   </b></td> <td>" + getReportingInstance().getReportPeriodDuration()  + "</td> </tr>\n");
			}
			else
			{
				sb.append(blankTableRow);
				sb.append("  <tr> " + tdBullet +" <td><b>Report Period: </b></td> <td> Full day</td> </tr>\n");
			}
			sb.append(blankTableRow);
			sb.append("  <tr> " + tdBullet +" <td><b>Recording Start Day:        </b></td> <td>" + _startDay               + "</td> </tr>\n");
			sb.append("  <tr> " + tdBullet +" <td><b>Recording Start Date:       </b></td> <td>" + _startTime              + "</td> </tr>\n");
			sb.append("  <tr> " + tdBullet +" <td><b>Recording End  Date:        </b></td> <td>" + _endTime                + "</td> </tr>\n");
			sb.append("  <tr> " + tdBullet +" <td><b>Recording Duration:         </b></td> <td>" + _duration               + "</td> </tr>\n");
			sb.append("  <tr> " + tdBullet +" <td><b>Recording Sample Time:      </b></td> <td>" + _recordingSampleTime    + "</td> </tr>\n");

			sb.append(blankTableRow);
			sb.append("  <tr> " + tdBullet +" <td><b>DBMS Server Name:           </b></td> <td>" + _dbmsServerName         + "</td> </tr>\n");
			if (StringUtil.hasValue(_dbmsDisplayName))
				sb.append("  <tr> " + tdBullet +" <td><b>DBMS Display Name:      </b></td> <td>" + _dbmsDisplayName        + "</td> </tr>\n");
			sb.append("  <tr> " + tdBullet +" <td><b>DBMS Version String:        </b></td> <td>" + _dbmsVersionString      + "</td> </tr>\n");

			// If the version String was changed during the recording...
			// So we experienced an UPGRADE... Lets make a notice about that...
			if (_dbmsVersionStringChangeTime != null)
			{
				sb.append(blankTableRow);
				sb.append("  <tr> " + tdBullet +" <td bgcolor='red'><b>DBMS Version String CHANGE Time: </b></td> <td>" + _dbmsVersionStringChangeTime + "</td> </tr>\n");
				sb.append("  <tr> " + tdBullet +" <td bgcolor='red'><b>DBMS MIN Version String:         </b></td> <td>" + _dbmsVersionStringMin        + "</td> </tr>\n");
				sb.append("  <tr> " + tdBullet +" <td bgcolor='red'><b>DBMS MAX Version String:         </b></td> <td>" + _dbmsVersionStringMax        + "</td> </tr>\n");
			}
			
			sb.append(blankTableRow);
   			sb.append("  <tr> " + tdBullet +" <td><b>DBMS Last Restart at Time:  </b></td> <td>" + _dbmsStartTimeStr       + "</td> </tr>\n");
			sb.append("  <tr> " + tdBullet +" <td><b>DBMS Last Restart in Days:  </b></td> <td>" + _dbmsStartTimeInDaysStr + "</td> </tr>\n");

			if (_dbmsOtherInfoMap != null && !_dbmsOtherInfoMap.isEmpty())
			{
				sb.append(blankTableRow);
				for (Entry<String, String> entry : _dbmsOtherInfoMap.entrySet())
				{
		   			sb.append("  <tr> " + tdBullet +" <td><b>" + entry.getKey() + ":  </b></td> <td>" + entry.getValue()   + "</td> </tr>\n");
				}
			}

			// OS info: CORE Count, MEMORY ...
			if (StringUtil.hasValue(_osCoreInfo) || StringUtil.hasValue(_osMemoryInfo))
			{
				if (StringUtil.hasValue(_dbmsDisplayName) && _dbmsDisplayName.contains("SQL Server"))
				{
					sb.append(blankTableRow);
					sb.append("  <tr> " + tdBullet +" <td><b>OS Core Info:            </b></td> <td>" + _osCoreInfo   + "</td> </tr>\n");
					sb.append("  <tr> " + tdBullet +" <td><b>OS Memory Info:          </b></td> <td>" + _osMemoryInfo + "</td> </tr>\n");
				}
				else
				{
					sb.append(blankTableRow);
					sb.append("  <tr> " + tdBullet +" <td><b>OS Core Count:            </b></td> <td>" + _osCoreInfo   + "</td> </tr>\n");
					sb.append("  <tr> " + tdBullet +" <td><b>OS Physical Memory:       </b></td> <td>" + _osMemoryInfo + "</td> </tr>\n");
				}
			}

			// Host monitoring
			sb.append(blankTableRow);
			sb.append("  <tr> " + tdBullet +" <td><b>Host Monitoring was Enabled:  </b></td> <td>" + _isHostMonitoringEnabled + "</td> </tr>\n");
			sb.append("  <tr> " + tdBullet +" <td><b>Host Monitoring hostname:     </b></td> <td>" + _hostMonitorHostname     + "</td> </tr>\n");

			// Java Version Info
			sb.append(blankTableRow);
			sb.append("  <tr> " + tdBullet +" <td><b>Java Version:                 </b></td> <td>" + System.getProperty("java.version")      + "</td> </tr>\n");
			sb.append("  <tr> " + tdBullet +" <td><b>Java Vendor:                  </b></td> <td>" + System.getProperty("java.vendor")       + "</td> </tr>\n");
			sb.append("  <tr> " + tdBullet +" <td><b>Java Home:                    </b></td> <td>" + System.getProperty("java.home")         + "</td> </tr>\n");
			sb.append("  <tr> " + tdBullet +" <td><b>Java Version Date:            </b></td> <td>" + System.getProperty("java.version.date") + "</td> </tr>\n");

			sb.append("</table>\n");
			
			
			// Get if HostMonitoring is enabled/disabled
			//  * if disabled: Write info on how to enable it
			//  * if Windows: write additional info on how to:
			//                - install SSH
			//                - create user 'dbxtune' or other user (with correct permissions)
			//                - test the above
			if ( ! _isHostMonitoringEnabled )
			{
				sb.append("<br> \n");
				sb.append("<b><i>Note:</i></b> Host monitoring is <b>not</b> enabled. If you want to get a closer look on various Operating System Counters, please enable it... see below.<br> \n");
				sb.append("The following commands is used to monitor the Operating System <i>(on Linux/Unix)</i> <br> \n");
				sb.append("<ul> \n");
				sb.append("  <li><code>mpstat</code            - For CPU activity</li> \n");
				sb.append("  <li><code>iostat</code            - For Disk activity</li> \n");
				sb.append("  <li><code>uptime</code            - For 'Average Load' or 'Average Run Queue Length' </li> \n");
				sb.append("  <li><code>vmstat</code            - For Swapping and CPU activity</li> \n");
				sb.append("  <li><code>df</code                - For Disk Usage</li> \n");
				sb.append("  <li><code>cat /proc/meminfo</code - For Memory Information</li> \n");
				sb.append("  <li><code>cat /proc/net/dev</code - For Network Activity</li> \n");
				sb.append("</ul> \n");
				sb.append("<br> \n");
				sb.append("To enable this functionality, do the following: \n");
				sb.append("In the start <i>wrapper</i> command/shellscript, found in <code>~/.dbxtune/dbxc/bin/start_<i>xxx</i>tune.sh</code> \n");
				sb.append("<ul> \n");
				sb.append("  <li>Specify switch <code>-u<i>osUsername</i></code> </li> \n");
				sb.append("  <li>Set the password using: <code>~/.dbxtune/dbxc/bin/dbxPassword.sh set -U<i>osUsername</i> -P<i>theSecretPasswd</i> -S<i>serverName</i></code></li> \n");
				sb.append("</ul> \n");
				sb.append("And enable desired Counter Models you want to collect in the config file: <code>~/.dbxtune/dbxc/conf/someName.conf</code> \n");
				sb.append("<ul> \n");
				sb.append("  <li><code>CmOsMpstat.persistCounters=true    </code></li> \n");
				sb.append("  <li><code>CmOsIostat.persistCounters=true    </code></li> \n");
				sb.append("  <li><code>CmOsUptime.persistCounters=true    </code></li> \n");
				sb.append("  <li><code>CmOsVmstat.persistCounters=true    </code>  <i> ## Note: This is <b>not</b> supported for Windows.</i> And do <b>NOT</b> add this comment in the config file!!!</li> \n");
				sb.append("  <li><code>CmOsDiskSpace.persistCounters=true </code></li> \n");
				sb.append("  <li><code>CmOsMeminfo.persistCounters=true   </code></li> \n");
				sb.append("  <li><code>CmOsNwInfo.persistCounters=true    </code></li> \n");
				sb.append("  <li><code>CmOsPs.persistCounters=true        </code></li> \n");
				sb.append("</ul> \n");
				sb.append("<br> \n");

				if (StringUtil.hasValue(_dbmsVersionString) && _dbmsVersionString.toLowerCase().contains("windows"))
				{
					sb.append("It looks like the DBMS is running on Windows ? <br> \n");
					sb.append("<i>&emsp; This asumption was made since the DBMS version string contained 'Windows'. DBMS Version String: '" + _dbmsVersionString + "'</i> <br> \n");
					sb.append("If this asumption is <b>correct</b>, then if you want to do host monitoring, you need to do the following on the Windows host where the DBMS is running on: \n");
					sb.append("<ul> \n");
					sb.append("  <li>Install a SSH (Secure Shell) on Windows. <br> \n");
					sb.append("  Here is a link how to do that <a href='https://docs.microsoft.com/en-us/windows-server/administration/openssh/openssh_install_firstuse'>https://docs.microsoft.com/en-us/windows-server/administration/openssh/openssh_install_firstuse</a>\n");
					sb.append("  </li>\n");

					sb.append("  <li>Create a user on the machine (Local or Active Directory user), below is an example of adding a Local user called 'dbxtune' <br>\n");
					sb.append("  <code>net user /add dbxtune someSecretPassword</code> <br> \n");
					sb.append("  </li> \n");

					sb.append("  <li>And grant some Authorizations to the above user, se example below<br> \n");
					sb.append("  <code>net localgroup \"Performance Log Users\" dbxtune /add</code> <br> \n");
					sb.append("  <code>net localgroup \"Distributed COM Users\" dbxtune /add</code> <br> \n");
					sb.append("  Or the below for becoming a <i>Local Admin</i> <br> \n");
					sb.append("  <code>net localgroup administrators dbxtune /add</code> <br> \n");
					sb.append("  </li> \n");
					sb.append("</ul> \n");

					sb.append("To test if the above works, you can open a DOS promt as user <i>dbxtune</i> and issue the below commands: \n");
					sb.append("<ul> \n");
					sb.append("  <li><code>typeperf -si 2 \"\\Memory\\*\"</code></li>\n");
					sb.append("  <li><code>powershell \"gwmi win32_logicaldisk | Format-Table\"</code></li>\n");
					sb.append("</ul> \n");

					sb.append("As a final test, you can test <i>end-to-end</i> by: \n");
					sb.append("<ul> \n");
					sb.append("  <li><code>ssh dbxtune@hostname-where-sqlserver-is-running.com</code></li>\n");
					sb.append("  <li>Then execute the two commands from the previous test section.</li>\n");
					sb.append("</ul> \n");
					sb.append("<br> \n");
				}
			}
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
		String schemaName = getReportingInstance().getDbmsSchemaName();

		return new String[] { PersistWriterBase.getTableName(null, schemaName, PersistWriterBase.SESSION_SAMPLES, null, false) };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		String sql;
		String schemaName = getReportingInstance().getDbmsSchemaName();

		if (hasReportingInstance())
		{
			DailySummaryReportAbstract dsr = getReportingInstance();
			_dbmsVersionString      = dsr.getDbmsVersionStr();
			_dbmsVersionStringMin   = dsr.getDbmsVersionStrMin();
			_dbmsVersionStringMax   = dsr.getDbmsVersionStrMax();
			_dbmsVersionStringChangeTime = dsr.getDbmsVersionStrChangeTime();
			_dbmsServerName         = dsr.getDbmsServerName();
			_dbmsStartTimeStr       = dsr.getDbmsStartTime()       == null ? "-unknown-" : dsr.getDbmsStartTime().toString();
			_dbmsStartTimeInDaysStr = dsr.getDbmsStartTimeInDays()     < 0 ? "-unknown-" : dsr.getDbmsStartTimeInDays()+"";
			_dbmsOtherInfoMap       = dsr.getDbmsOtherInfoMap();
			_recordingVersion       = dsr.getRecDbxAppName() + ", Version: " + dsr.getRecDbxVersionStr() + ", Build: " + dsr.getRecDbxBuildStr();

			if (dsr.getReportContent() != null)
				_dbmsDisplayName = dsr.getReportContent().getDisplayOrServerName();
		}
		
		//-----------------------------------------
		// Get "sample time" for the recording
		//-----------------------------------------
		_recordingSampleTime = StringUtil.parseInt(getRecordingSessionParameter(conn, null, "offline.sampleTime"), -1);
		//_recordingSampleTime = getRecordingSampleTime(conn);

		//-----------------------------------------
		// Get Host Monitoring HOSTNAME
		//-----------------------------------------
		_hostMonitorHostname = getRecordingSessionParameter(conn, null, "conn.sshHostname");

		//-----------------------------------------
		// OS CoreCound & MemoryInfo
		//-----------------------------------------
		_osCoreInfo   = getOsCoreInfo(conn);
		_osMemoryInfo = getOsMemoryInfo(conn);

		//-----------------------------------------
		// Get/Check if Host Monitoring was enabled
		//-----------------------------------------
		_isHostMonitoringEnabled = isHostMonitoringEnabled(conn);

		//-----------------------------------------
		// Start/end time for the recording
		//-----------------------------------------
		sql = "select min([SessionSampleTime]), max([SessionSampleTime]) \n" +
		      "from " + PersistWriterBase.getTableName(conn, schemaName, PersistWriterBase.SESSION_SAMPLES, null, true) + " \n";

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
					
					_startTimeTs = startTime;
					_endTimeTs   = endTime;

					if (startTime != null && endTime != null)
					{
						_startTime = TimeUtils.toStringYmdHms(startTime);
						_endTime   = TimeUtils.toStringYmdHms(endTime);
//						_duration  = TimeUtils.msToTimeStr("%HH:%MM:%SS", endTime.getTime() - startTime.getTime() );
						_duration  = TimeUtils.msToTimeStrDHMS(endTime.getTime() - startTime.getTime() );
						_startDay  = startTime.toLocalDateTime().getDayOfWeek().name();
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

	private boolean isHostMonitoringEnabled(DbxConnection conn)
	{
		// Check if any of the tables exists... exit as soon as you find a table
		if (doTableExist(conn, null, "CmOsDiskSpace_abs")) return true;
		if (doTableExist(conn, null, "CmOsIostat_abs"   )) return true;
		if (doTableExist(conn, null, "CmOsMeminfo_abs"  )) return true;
		if (doTableExist(conn, null, "CmOsMpstat_abs"   )) return true;
		if (doTableExist(conn, null, "CmOsNwInfo_abs"   )) return true;
		if (doTableExist(conn, null, "CmOsUptime_abs"   )) return true;
		if (doTableExist(conn, null, "CmOsVmstat_abs"   )) return true;
		
		return false;
	}

	/**
	 * Get OS Memory Information (and SQL Server specifics, is available)
	 * @param conn A Connection the the PCS Storage
	 * @return A String with various information
	 */
	private String getOsMemoryInfo(DbxConnection conn)
	{
		// Get SQL Server info
		String sqlServerInfo = "";
		{
			Map<String, String> sysInfo = getSqlServerConfig_SysInfo(conn);
			if (sysInfo != null && !sysInfo.isEmpty())
			{
				long   physical_memory_kb       = StringUtil.parseLong(sysInfo.get("physical_memory_kb"), 0);
				long   cfg_max_server_memory_MB = StringUtil.parseLong(sysInfo.get("max server memory (MB)"), 0);
				String sql_memory_model_desc    = sysInfo.get("sql_memory_model_desc");

				double physical_memory_mb = NumberUtils.round((physical_memory_kb / 1024.0)         , 1);
				double physical_memory_gb = NumberUtils.round((physical_memory_kb / 1024.0 / 1024.0), 1);

				sqlServerInfo = "dm_os_sys_info: physical_memory_GB=" + physical_memory_gb + ", physical_memory_MB=" + physical_memory_mb + ", sql_memory_model_desc=" + sql_memory_model_desc + ", SrvConfig: 'max server memory (MB)'=" + cfg_max_server_memory_MB;
//				if ( isWindows() )
//				{
//					sqlServerInfo += "<br><b>Note:</b> dm_os_sys_info.physical_memory_XX is <i>in my experiance</i> only 80% of the real physical memory on Windows.";
//				}
			}
		}

		// NOT SQL-Server and on Windows ... NOT Supported
		if (isWindows() && StringUtil.isNullOrBlank(sqlServerInfo))
		{
			return "Physical Memory on Windows is not supported right now, sorry. (only supported for SQL Server)";
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
				output += "<br>";

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
		if (StringUtil.hasValue(_dbmsVersionString) && _dbmsVersionString.contains("SQL Server"))
		{
			Map<String, String> sysInfo = getSqlServerConfig_SysInfo(conn);
			if (sysInfo != null && !sysInfo.isEmpty())
			{
				String cpu_count                 = "cpu_count="                 + sysInfo.get("cpu_count");
				String hyperthread_ratio         = "hyperthread_ratio="         + sysInfo.get("hyperthread_ratio");
				String socket_count              = "socket_count="              + sysInfo.get("socket_count");
				String cores_per_socket          = "cores_per_socket="          + sysInfo.get("cores_per_socket");
				String numa_node_count           = "numa_node_count="           + sysInfo.get("numa_node_count");
				String virtual_machine_type_desc = "virtual_machine_type_desc=" + sysInfo.get("virtual_machine_type_desc");
				
				sqlServerInfo = "dm_os_sys_info: " + StringUtil.toCommaStr(cpu_count, hyperthread_ratio, socket_count, cores_per_socket, numa_node_count, virtual_machine_type_desc);
			}
		}

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
				output += "<br>";

			output += "SqlServerInfo: " + sqlServerInfo;
		}
		
		return output;
	}
	
	/**
	 * Get SQL Server information from the PCS Config subsystem (all available columns as a Map from sys.dm_os_sys_info)
	 * @param conn A Connection the the PCS Storage
	 * @return A Map with various information
	 */
	private Map<String, String> getSqlServerConfig_SysInfo(DbxConnection conn)
	{
		Map<String, String> sqlServerSysInfoMap = new LinkedHashMap<>();
		String sqlServerSysInfo = null;

		String schemaNamePrefix = getReportingInstance().getDbmsSchemaNameSqlPrefix();

		String sql = ""
			+ "select [configText] \n"
			+ "from " + schemaNamePrefix + "[MonSessionDbmsConfigText] \n"
			+ "where [configName] = 'SqlServerSysInfo' \n"
		    + "  and [SessionStartTime] = (select max([SessionStartTime]) from " + schemaNamePrefix + "[MonSessionDbmsConfigText]) \n"
			+ "";
				
		sql = conn.quotifySqlString(sql);
		try ( Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while (rs.next())
			{
				sqlServerSysInfo = rs.getString(1);
			}
		}
		catch(SQLException ex)
		{
			_logger.warn("Problems getting: 'getSqlServerConfig_SysInfo()', Caught: " + ex);
			sqlServerSysInfo = null;
		}

		if (sqlServerSysInfo != null)
		{
			ResultSetTableModel rstm = ResultSetTableModel.parseTextTable(sqlServerSysInfo);
			for (int c=0; c<rstm.getColumnCount(); c++)
			{
				String colName = rstm.getColumnName(c);
				String colval  = rstm.getValueAsString(0, c);
				
				sqlServerSysInfoMap.put(colName, colval);
			} 

			// Can we also get 'max server memory (MB)' from config...
			sql = ""
					+ "select [Name], [RunValue] \n"
					+ "from " + schemaNamePrefix + "[MonSessionDbmsConfig] \n"
					+ "where [Name] = 'max server memory (MB)' \n"
				    + "  and [SessionStartTime] = (select max([SessionStartTime]) from " + schemaNamePrefix + "[MonSessionDbmsConfig]) \n"
					+ "";

			sql = conn.quotifySqlString(sql);
			try ( Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while (rs.next())
				{
					String colName = rs.getString(1);
					String colval  = rs.getString(2);
					
					sqlServerSysInfoMap.put(colName, colval);
				}
			}
			catch(SQLException ex)
			{
				_logger.warn("Problems getting: 'getSqlServerConfig_SysInfo()::sp_config_values', Caught: " + ex);
			}
		}
		
		return sqlServerSysInfoMap;
	}

}
