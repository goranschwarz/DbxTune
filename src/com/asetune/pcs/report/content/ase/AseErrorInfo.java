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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.asetune.config.dict.AseErrorMessageDictionary;
import com.asetune.gui.ModelMissmatchException;
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
	private ResultSetTableModel _sqlTextRstm;
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

			if (_sqlTextRstm != null)
			{
				// put "xmp" tags around the data: <xmp>cellContent</xmp>, for some columns
				Map<String, String> colNameValueTagMap = new HashMap<>();
				colNameValueTagMap.put("SQLText", "xmp");

				String  divId       = "errorSqlText";
				boolean showAtStart = false;
				String  htmlContent = _sqlTextRstm.toHtmlTableString("sortable", colNameValueTagMap);
				
				String showHideDiv = createShowHideDiv(divId, showAtStart, "Show/Hide Error SQL Text, for above errors (all text's may not be available)...", htmlContent);

				// Compose special condition for Microsoft Outlook
				sb.append(msOutlookAlternateText("Error Text", showHideDiv));
			}
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
				"<br>" +
				"<b>Note</b>: To view the SQL Statement that was responsible for the error(s), There is a table <i>which is collapsed</i> where they can be viewed.<br>" +
				"&emsp; You can also connect to the Detailed Recording Database. (Using the Desktop Version of " + getReportingInstance().getDbxAppName() + "), use 'Tools-&gt;Captue SQL Offline View...' <br>" +
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

		int skipErrorCountAbove = conf.getIntProperty("AseErrorInfo.skip.ErrorCountAbove", 2000);
		_messages.add("Skipping SQL Errors: SQL Text, if 'ErrorCount' is above " + skipErrorCountAbove + ". This can be changed with property 'AseErrorInfo.skip.ErrorCountAbove = ####'");
		
		String sql = ""
			    + "select \n"
			    + "  [ErrorStatus]            as [ErrorStatus] \n"
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

			// set duration
			setDurationColumn(_shortRstm, "FirstEntry", "LastEntry", "Duration");
			
			// Fill in the "ErrorMessage" column from a Static Dictionary
			int pos_ErrorStatus  = _shortRstm.findColumn("ErrorStatus");
			int pos_ErrorCount   = _shortRstm.findColumn("ErrorCount");
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

			// Get SQL Details for a specific error number
			if (pos_ErrorStatus >= 0 && pos_ErrorCount >= 0 && pos_ErrorMessage >= 0)
			{
				for (int r=0; r<_shortRstm.getRowCount(); r++)
				{
					int ErrorStatus = _shortRstm.getValueAsInteger(r, pos_ErrorStatus);
					int ErrorCount  = _shortRstm.getValueAsInteger(r, pos_ErrorCount);

					if (ErrorCount < skipErrorCountAbove)
						getErrorSqlText(conn, ErrorStatus);
				}
			}
		}
	}

	private void getErrorSqlText(DbxConnection conn, int errorNumber)
	{
		String sql = ""
			    + "select s.[ErrorStatus], count(t.*) as [ErrorCount], t.[ServerLogin], s.[ProcName], s.[LineNumber], t.[JavaSqlHashCode], min(t.[SQLText]) as [SQLText] \n"
			    + "from [MonSqlCapStatements] s \n"
			    + "INNER JOIN [MonSqlCapSqlText] t ON s.[SPID] = t.[SPID] and s.[KPID] = t.[KPID] and s.[BatchID] = t.[BatchID] \n"
			    + "where s.[ErrorStatus] != 0 \n"
//			    + "and s.[JavaSqlHashCode] != -1 \n"
			    + "and s.[ErrorStatus] = " + errorNumber + " \n"
			    + "group by s.[ErrorStatus], t.[ServerLogin], s.[ProcName], s.[LineNumber], t.[JavaSqlHashCode] \n"
			    + "order by s.[ErrorStatus], [ErrorCount] desc \n"
			    + "";

		sql = conn.quotifySqlString(sql);
		try ( Statement stmnt = conn.createStatement() )
		{
			// Unlimited execution time
			stmnt.setQueryTimeout(0);
			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				ResultSetTableModel rstm = createResultSetTableModel(rs, "SqlErrorText", sql);
				
				if (_sqlTextRstm == null)
					_sqlTextRstm = rstm;
				else
					_sqlTextRstm.add(rstm);
			}
		}
		catch(SQLException ex)
		{
			setProblem(ex);

			_logger.warn("Problems getting ErrorSqlText for ErrorStatus = "+errorNumber+": " + ex);
		} 
		catch(ModelMissmatchException ex)
		{
			setProblem(ex);

			_logger.warn("Problems (merging into previous ResultSetTableModel) when getting ErrorSqlText for ErrorStatus = "+errorNumber+": " + ex);
		} 
	}
}
