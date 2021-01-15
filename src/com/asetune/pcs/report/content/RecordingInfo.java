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
package com.asetune.pcs.report.content;

import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import org.apache.log4j.Logger;

import com.asetune.Version;
import com.asetune.pcs.PersistWriterBase;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.TimeUtils;

public class RecordingInfo
extends ReportEntryAbstract
{
	private static Logger _logger = Logger.getLogger(RecordingInfo.class);

	private Timestamp _startTimeTs = null;
	private Timestamp _endTimeTs   = null;
	private String    _startTime   = null;
	private String    _endTime     = null;
	private String    _duration    = null;

	private Exception _problem     = null;

	private String _dbmsVersionString;
	private String _dbmsServerName;
	private String _dbmsStartTimeStr;
	private String _dbmsStartTimeInDaysStr;
	
	private String _reportVersion    = Version.getAppName() + ", Version: " + Version.getVersionStr() + ", Build: " + Version.getBuildStr();
	private String _recordingVersion = null;
	
	private int    _recordingSampleTime = -1;

	public RecordingInfo(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	public Timestamp getStartTimeTs()         { return _startTimeTs; } 
	public Timestamp getEndTimeTs()           { return _endTimeTs; } 
	public String    getStartTime()           { return _startTime; } 
	public String    getEndTime()             { return _endTime; } 
	public String    getDuration()            { return _duration; } 
	public String    getDbmsVersionString()   { return _dbmsVersionString; } 
	public String    getDbmsServerName()      { return _dbmsServerName; } 
	
	@Override
	public boolean hasShortMessageText()
	{
		return true;
	}

	@Override
	public void writeShortMessageText(Writer w)
	throws IOException
	{
		writeMessageText(w);
	}

	@Override
	public void writeMessageText(Writer sb)
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
			sb.append("  <tr> " + tdBullet +" <td><b>Recording Start Date:       </b></td> <td>" + _startTime              + "</td> </tr>\n");
			sb.append("  <tr> " + tdBullet +" <td><b>Recording End  Date:        </b></td> <td>" + _endTime                + "</td> </tr>\n");
			sb.append("  <tr> " + tdBullet +" <td><b>Recording Duration:         </b></td> <td>" + _duration               + "</td> </tr>\n");
			sb.append("  <tr> " + tdBullet +" <td><b>Recording Sample Time:      </b></td> <td>" + _recordingSampleTime    + "</td> </tr>\n");

			sb.append(blankTableRow);
			sb.append("  <tr> " + tdBullet +" <td><b>DBMS Server Name:           </b></td> <td>" + _dbmsServerName         + "</td> </tr>\n");
			sb.append("  <tr> " + tdBullet +" <td><b>DBMS Version String:        </b></td> <td>" + _dbmsVersionString      + "</td> </tr>\n");

			sb.append(blankTableRow);
   			sb.append("  <tr> " + tdBullet +" <td><b>DBMS Last Restart at Time:  </b></td> <td>" + _dbmsStartTimeStr       + "</td> </tr>\n");
			sb.append("  <tr> " + tdBullet +" <td><b>DBMS Last Restart in Days:  </b></td> <td>" + _dbmsStartTimeInDaysStr + "</td> </tr>\n");
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
		return new String[] { PersistWriterBase.getTableName(null, PersistWriterBase.SESSION_SAMPLES, null, false) };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		if (hasReportingInstance())
		{
			DailySummaryReportAbstract dsr = getReportingInstance();
			_dbmsVersionString      = dsr.getDbmsVersionStr();
			_dbmsServerName         = dsr.getDbmsServerName();
			_dbmsStartTimeStr       = dsr.getDbmsStartTime()       == null ? "-unknown-" : dsr.getDbmsStartTime().toString();
			_dbmsStartTimeInDaysStr = dsr.getDbmsStartTimeInDays()     < 0 ? "-unknown-" : dsr.getDbmsStartTimeInDays()+"";
			_recordingVersion       = dsr.getRecDbxAppName() + ", Version: " + dsr.getRecDbxVersionStr() + ", Build: " + dsr.getRecDbxBuildStr();
		}

		_recordingSampleTime = getRecordingSampleTime(conn);

		
		String sql;

		// Start/end time for the recording
		sql = "select min([SessionSampleTime]), max([SessionSampleTime]) \n" +
		      "from ["+PersistWriterBase.getTableName(conn, PersistWriterBase.SESSION_SAMPLES, null, false) + "] \n";

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
						_startTime = startTime + "";
						_endTime   = endTime   + "";
						_duration  = TimeUtils.msToTimeStr("%HH:%MM:%SS", endTime.getTime() - startTime.getTime() );
					//	_duration  = TimeUtils.msToTimeStr("%?DD[d ]%HH:%MM:%SS", endTime.getTime() - startTime.getTime() );
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
}
