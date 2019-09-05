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
package com.asetune.pcs.report.content;

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

	private String    _startTime = null;
	private String    _endTime   = null;
	private String    _duration  = null;

	private Exception _problem   = null;

	private String _dbmsVersionString;
	private String _dbmsServerName;
	
	private String _reportVersion    = Version.getAppName() + ", Version: " + Version.getVersionStr() + ", Build: " + Version.getBuildStr();
	private String _recordingVersion = null;

	public RecordingInfo(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	public String getStartTime()           { return _startTime; } 
	public String getEndTime()             { return _endTime; } 
	public String getDuration()            { return _duration; } 
	public String getDbmsVersionString()   { return _dbmsVersionString; } 
	public String getDbmsServerName()      { return _dbmsServerName; } 
	
	@Override
	public String getMsgAsText()
	{
		StringBuilder sb = new StringBuilder();

		if (_startTime == null)
		{
			sb.append("Can't get start/end time \n");
		}
		else
		{
			sb.append("  Start:               " + _startTime + "\n");
			sb.append("  End:                 " + _endTime   + "\n");
			sb.append("  Duration:            " + _duration  + "\n");
			sb.append("  DBMS Server Name:    " + _dbmsServerName     + "\n");
			sb.append("  DBMS Version String: " + _dbmsVersionString  + "\n");
		}

		if (_problem != null)
			sb.append(_problem);
		
		return sb.toString();
	}

	@Override
	public String getMsgAsHtml()
	{
		StringBuilder sb = new StringBuilder();

		if (_startTime == null)
		{
			sb.append("Can't get start/end time <br>\n");
		}
		else
		{			
			sb.append("<ul>\n");
			sb.append("  <li><b>Recording was Made Using:   </b>" + _recordingVersion   + "</li>\n");
			sb.append("  <li><b>The Report is Produced by : </b>" + _reportVersion      + "</li>\n");
			sb.append("<br>\n");
			sb.append("  <li><b>Recording Start Date:       </b>" + _startTime          + "</li>\n");
			sb.append("  <li><b>Recording End  Date:        </b>" + _endTime            + "</li>\n");
			sb.append("  <li><b>Recording Duration:         </b>" + _duration           + "</li>\n");
			sb.append("<br>\n");
			sb.append("  <li><b>DBMS Server Name:           </b>" + _dbmsServerName     + "</li>\n");
			sb.append("  <li><b>DBMS Version String:        </b>" + _dbmsVersionString  + "</li>\n");
			sb.append("</ul>\n");
		}

		if (_problem != null)
			sb.append("<pre>").append(_problem).append("</pre> \n");

		sb.append("\n<br>");

		return sb.toString();
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
	public void create(DbxConnection conn, String srvName, Configuration conf)
	{
		if (hasReportingInstance())
		{
			DailySummaryReportAbstract dsr = getReportingInstance();
			_dbmsVersionString = dsr.getDbmsVersionStr();
			_dbmsServerName    = dsr.getDbmsServerName();
			_recordingVersion = dsr.getRecDbxAppName() + ", Version: " + dsr.getRecDbxVersionStr() + ", Build: " + dsr.getRecDbxBuildStr();
		}
		
		String sql;

		// Get Alarms
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
