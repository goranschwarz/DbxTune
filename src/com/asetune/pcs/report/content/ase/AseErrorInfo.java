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
package com.asetune.pcs.report.content.ase;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.asetune.config.dict.AseErrorMessageDictionary;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;

public class AseErrorInfo extends AseAbstract
{
	private static Logger _logger = Logger.getLogger(AseErrorInfo.class);
	
	private ResultSetTableModel _shortRstm;
//	private Exception           _problem = null;
	private List<String>        _messages = new ArrayList<>();

	public AseErrorInfo(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public String getMsgAsText()
	{
		StringBuilder sb = new StringBuilder();

		if (_messages.size() > 0)
		{
			sb.append("Messages: \n");
			for (String msg : _messages)
				sb.append("  * ").append(msg).append(" \n");
		}

		if (_shortRstm.getRowCount() == 0)
		{
			sb.append("No Error Information \n");
		}
		else
		{
			sb.append("Error Information Count: ").append(_shortRstm.getRowCount()).append("\n");
			sb.append(_shortRstm.toAsciiTableString());
		}

		if (hasProblem())
			sb.append(getProblem());
		
		return sb.toString();
	}

	@Override
	public String getMsgAsHtml()
	{
		StringBuilder sb = new StringBuilder();

		if (_messages.size() > 0)
		{
			sb.append("<b>Messages:</b> \n");
			sb.append("<ul> \n");
			for (String msg : _messages)
				sb.append("  <li>").append(msg).append("</li> \n");
			sb.append("</ul> \n");
		}

		if (_shortRstm.getRowCount() == 0)
		{
			sb.append("No Error Information <br>\n");
		}
		else
		{
			// Get a description of this section, and column names
			sb.append(getSectionDescriptionHtml(_shortRstm, true));

			sb.append("Error Information Count: ").append(_shortRstm.getRowCount()).append("<br>\n");
			sb.append(_shortRstm.toHtmlTableString("sortable"));
		}

		if (hasProblem())
			sb.append("<pre>").append(getProblem()).append("</pre> \n");

		sb.append("\n<br>");

		return sb.toString();
	}

	@Override
	public String getSubject()
	{
		return "Error Information (order by: ErrorCount)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}

	/**
	 * Set descriptions for the table, and the columns
	 */
	private void setSectionDescription(ResultSetTableModel rstm)
	{
		if (rstm == null)
			return;
		
		// Section description
		rstm.setDescription(
				"Errors that ended up in the MDA Table 'monSysStatements' with 'ErrorStatus' != 0 <br>" +
				"This can be used to drilldown or pinpoint troublesome SQL Statements. <br>" +
				"<b>Note</b>: To view the SQL Statement that was responsible for the error(s), you need to connect to the Detailed Recording Database. (Using the Desktop Version of " + getReportingInstance().getDbxAppName() + "), use 'Tools-&gt;Captue SQL Offline View...' <br>" +
				"");

		// Columns description
		rstm.setColumnDescription("ErrorStatus" , "ASE Error Number");
		rstm.setColumnDescription("ErrorCount"  , "Number of records found");
		rstm.setColumnDescription("SpidCount"   , "Number if different SPID's this message occurred for");
		rstm.setColumnDescription("FirstEntry"  , "Time of first occurrence, this day");
		rstm.setColumnDescription("LastEntry"   , "Time of last occurrence, this day");
		rstm.setColumnDescription("Duration"    , "To pinpoint if the errors occurred during a specific time on the day");
		rstm.setColumnDescription("ErrorMessage", "ASE Message description (from master.dbo.sysmessages)");
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration conf)
	{
		Set<String> skipErrorNumbers = StringUtil.parseCommaStrToSet( conf.getProperty("AseErrorInfo.skip.errors", "") );
		
		String skipErrorsWhereClause = "";
		if ( ! skipErrorNumbers.isEmpty() )
		{
			skipErrorsWhereClause = " and [ErrorStatus] not in(" + StringUtil.toCommaStr(skipErrorNumbers) + ") \n";

			_messages.add("Skipping error numbers " + skipErrorNumbers);

			_logger.info("Skipping error numbers " + skipErrorNumbers + " for server '" + srvName + "'.");
		}
		
		String sql = ""
			    + "select [ErrorStatus] \n"
			    + "	,count(*)                 as [ErrorCount] \n"
			    + "	,count(distinct [SPID])   as [SpidCount] \n"
			    + "	,min([StartTime])         as [FirstEntry] \n"
			    + "	,max([StartTime])         as [LastEntry] \n"
			    + "	,cast('' as varchar(30))  as [Duration] \n"
			    + "	,cast('' as varchar(255)) as [ErrorMessage] \n"
			    + "from [MonSqlCapStatements] \n"
			    + "where [ErrorStatus] > 0 \n"
			    + skipErrorsWhereClause
			    + "group by [ErrorStatus] \n"
			    + "order by [ErrorCount] desc \n"
			    + "";

		_shortRstm = executeQuery(conn, sql, false, "SQL With ErrorStatus");
		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("SQL With ErrorStatus");
			return;
		}
		else
		{
			// Describe the table
			setSectionDescription(_shortRstm);

			
			// Fill in the "ErrorMessage" column from a Static Dictionary
			int pos_ErrorStatus  = _shortRstm.findColumn("ErrorStatus");
			int pos_ErrorMessage = _shortRstm.findColumn("ErrorMessage");

			if (pos_ErrorStatus >= 0 && pos_ErrorMessage >= 0)
			{
				for (int r=0; r<_shortRstm.getRowCount(); r++)
				{
					int ErrorStatus = _shortRstm.getValueAsInteger(r, pos_ErrorStatus);

					String msgStr = AseErrorMessageDictionary.getInstance().getDescription(ErrorStatus);
					_shortRstm.setValueAtWithOverride(msgStr, r, pos_ErrorMessage);
				}
			}

			int pos_FirstEntry = _shortRstm.findColumn("FirstEntry");
			int pos_LastEntry  = _shortRstm.findColumn("LastEntry");
			int pos_Duration   = _shortRstm.findColumn("Duration");

			if (pos_FirstEntry >= 0 && pos_LastEntry >= 0 && pos_Duration >= 0)
			{
				for (int r=0; r<_shortRstm.getRowCount(); r++)
				{
					Timestamp FirstEntry = _shortRstm.getValueAsTimestamp(r, pos_FirstEntry);
					Timestamp LastEntry  = _shortRstm.getValueAsTimestamp(r, pos_LastEntry);

					if (FirstEntry != null && LastEntry != null)
					{
						long durationInMs = LastEntry.getTime() - FirstEntry.getTime();
						String durationStr = TimeUtils.msToTimeStr("%HH:%MM:%SS", durationInMs);
						_shortRstm.setValueAtWithOverride(durationStr, r, pos_Duration);
					}
				}
			}
		}

//		sql = conn.quotifySqlString(sql);
//		try ( Statement stmnt = conn.createStatement() )
//		{
//			// Unlimited execution time
//			stmnt.setQueryTimeout(0);
//			try ( ResultSet rs = stmnt.executeQuery(sql) )
//			{
////				_shortRstm = new ResultSetTableModel(rs, "SQL With ErrorStatus");
//				_shortRstm = createResultSetTableModel(rs, "SQL With ErrorStatus");
//				
//				// Fill in the "ErrorMessage" column from a Static Dictionary
//				int pos_ErrorStatus  = _shortRstm.findColumn("ErrorStatus");
//				int pos_ErrorMessage = _shortRstm.findColumn("ErrorMessage");
////System.out.println("pos_ErrorStatus="+pos_ErrorStatus+", pos_ErrorMessage="+pos_ErrorMessage);
//
//				if (pos_ErrorStatus >= 0 && pos_ErrorMessage >= 0)
//				{
//					for (int r=0; r<_shortRstm.getRowCount(); r++)
//					{
//						int ErrorStatus = _shortRstm.getValueAsInteger(r, pos_ErrorStatus);
//
//						String msgStr = AseErrorMessageDictionary.getInstance().getDescription(ErrorStatus);
////System.out.println("MSG-num2desc-RESOLV: r="+r+", ErrorStatus="+ErrorStatus+", msgStr="+msgStr);
//						_shortRstm.setValueAtWithOverride(msgStr, r, pos_ErrorMessage);
//					}
//				}
//
//				int pos_FirstEntry = _shortRstm.findColumn("FirstEntry");
//				int pos_LastEntry  = _shortRstm.findColumn("LastEntry");
//				int pos_Duration   = _shortRstm.findColumn("Duration");
//
//				if (pos_FirstEntry >= 0 && pos_LastEntry >= 0 && pos_Duration >= 0)
//				{
//					for (int r=0; r<_shortRstm.getRowCount(); r++)
//					{
//						Timestamp FirstEntry = _shortRstm.getValueAsTimestamp(r, pos_FirstEntry);
//						Timestamp LastEntry  = _shortRstm.getValueAsTimestamp(r, pos_LastEntry);
//
//						if (FirstEntry != null && LastEntry != null)
//						{
//							long durationInMs = LastEntry.getTime() - FirstEntry.getTime();
//							String durationStr = TimeUtils.msToTimeStr("%HH:%MM:%SS", durationInMs);
//							_shortRstm.setValueAtWithOverride(durationStr, r, pos_Duration);
//						}
//					}
//				}
//				
//				if (_logger.isDebugEnabled())
//					_logger.debug("_alarmsActiveShortRstm.getRowCount()="+ _shortRstm.getRowCount());
//			}
//		}
//		catch(SQLException ex)
//		{
//			_problem = ex;
//
//			_shortRstm = ResultSetTableModel.createEmpty("Error Information Short");
//			_logger.warn("Problems getting Alarms Short: " + ex);
//		}
	}
}
