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

import com.asetune.CounterController;
import com.asetune.cm.CountersModel;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.DailySummaryReportFactory;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;

public abstract class ReportEntryAbstract
implements IReportEntry
{
	private static Logger _logger = Logger.getLogger(ReportEntryAbstract.class);

	private Exception _problem;
	protected DailySummaryReportAbstract _reportingInstance;

	public ReportEntryAbstract(DailySummaryReportAbstract reportingInstance)
	{
		_reportingInstance = reportingInstance;
	}

	public boolean hasReportingInstance()
	{
		return _reportingInstance != null;
	}

	public DailySummaryReportAbstract getReportingInstance()
	{
		return _reportingInstance;
	}

	@Override
	public boolean isEnabled()
	{
		String  key          = getIsEnabledConfigKeyName();
		boolean defaultValue = DailySummaryReportFactory.DEFAULT_isReportEntryEnabled;

		return Configuration.getCombinedConfiguration().getBooleanProperty(key, defaultValue);
	}
	@Override
	public String getIsEnabledConfigKeyName()
	{
		String key = DailySummaryReportFactory.PROPKEY_isReportEntryEnabled;

		key = key.replace("<ENTRY-NAME>", this.getClass().getSimpleName());
		
		return key;
	}
	@Override
	public boolean canBeDisabled()
	{
		return true;
	}


	public ResultSetTableModel createResultSetTableModel(ResultSet rs, String name, String sql)
	throws SQLException
	{
		return createResultSetTableModel(rs, name, sql, true);
	}
	public ResultSetTableModel createResultSetTableModel(ResultSet rs, String name, String sql, boolean doTruncate)
	throws SQLException
	{
		ResultSetTableModel rstm = new ResultSetTableModel(rs, name, sql);

		// Set toString format for Timestamp to "yyyy-MM-dd HH:mm:ss"
		rstm.setToStringTimestampFormat_YMD_HMS();

		// use localized numbers to easier see big numbers (for example: 12345 -> 12,345)
		rstm.setToStringNumberFormat(true);

		// Truncate *long* columns
		if (doTruncate)
		{
			int truncLongCellSize = Configuration.getCombinedConfiguration().getIntProperty(DailySummaryReportFactory.PROPKEY_maxTableCellSizeKb, DailySummaryReportFactory.DEFAULT_maxTableCellSizeKb);
			rstm.truncateColumnsWithSizeInKbOver(truncLongCellSize);
		}
		
		return rstm;
	}


	public ResultSetTableModel executeQuery(DbxConnection conn, String sql, boolean onErrorCreateEmptyRstm, String name)
	{
		return executeQuery(conn, sql, onErrorCreateEmptyRstm, name, true);
	}
	public ResultSetTableModel executeQuery(DbxConnection conn, String sql, boolean onErrorCreateEmptyRstm, String name, boolean doTruncate)
	{
		// transform all "[" and "]" to DBMS Vendor Quoted Identifier Chars 
		sql = conn.quotifySqlString(sql);

		try ( Statement stmnt = conn.createStatement() )
		{
			// Unlimited execution time
			stmnt.setQueryTimeout(0);
			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				ResultSetTableModel rstm = createResultSetTableModel(rs, name, sql, doTruncate);
				
				if (_logger.isDebugEnabled())
					_logger.debug(name + "rstm.getRowCount()="+ rstm.getRowCount());
				
				return rstm;
			}
		}
		catch(SQLException ex)
		{
			setProblem(ex);
			
			//_fullRstm = ResultSetTableModel.createEmpty(name);
			_logger.warn("Problems getting '" + name + "': " + ex);
			
			if (onErrorCreateEmptyRstm)
				return ResultSetTableModel.createEmpty(name);
			else
				return null;
		}
	}
	
	@Override
	public String getEndOfReportText()
	{
		return "\n<br>\n";
		
	}

	@Override
	public String getProblemText()
	{
		if ( ! hasProblem() )
			return "";

		StringBuilder sb = new StringBuilder();
		
		Exception ex = getProblem();
		sb.append("<pre>").append(ex).append("</pre> \n");
		
		if (isPrintStacktraceEnabled())
		{
			sb.append("<b>Stacktrace:</b><br> \n");
			sb.append("<pre>").append( StringUtil.exceptionToString(ex)).append("</pre> \n");
		}
		
		return sb.toString();
	}
	
	public boolean isPrintStacktraceEnabled()
	{
		return true;
	}

	public void setProblem(Exception ex)
	{
		_problem = ex;
	}
	public Exception getProblem()
	{
		return _problem;
	}
	public boolean hasProblem()
	{
		return _problem != null;
	}


	/**
	 * Create a description of this report section<br>
	 * Column names should be described in a list.
	 * @return
	 */
	public String getSectionDescriptionHtml(ResultSetTableModel rstm, boolean addSqlText)
	{
		if (rstm == null)
			return "";
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("<p>\n");

		String desc = rstm.getDescription();
		if (desc != null)
		{
			// Strip off <html> start/end
			if (desc.substring(0, 10).toLowerCase().startsWith("<html>"))
			{
				desc = desc.substring("<html>".length());
				desc = StringUtil.removeLastStr(desc, "</html>");
			}
			sb.append(desc);
		}
		
		if (rstm.hasColumnDescriptions() && rstm.getColumnNames() != null)
		{
			String  divId       = "colDesc_" + StringUtil.stripAllNonAlphaNum(rstm.getName());
			boolean showAtStart = false;

			// as: UL - Unordered List
//			String htmlContent = "<ul> \n";
//			for (String colName : rstm.getColumnNames())
//				htmlContent += "  <li><b>" + colName + "</b> - " + rstm.getColumnDescription(colName) + "</li> \n";
//			htmlContent += "</ul> \n";

			// as: HTML Table
			String htmlContent = "<table> \n";
			htmlContent += "<thead> \n";
			htmlContent += "  <tr> <th nowrap>Column Name</th> <th nowrap>Description</th> </tr> \n";
			htmlContent += "</thead> \n";
			htmlContent += "<tbody> \n";
			for (String colName : rstm.getColumnNames())
				htmlContent += "  <tr> <td nowrap><b>" + colName + "</b></td>  <td nowrap>" + rstm.getColumnDescription(colName) + "</td> </tr> \n";
			htmlContent += "</tbody> \n";
			htmlContent += "</table> \n";

			String showHideDiv = createShowHideDiv(divId, showAtStart, "Show/Hide Column(s) description...", htmlContent);
			
			sb.append( msOutlookAlternateText(showHideDiv, "Column Desciption", "") );
		}

		if (addSqlText)
		{
			String sqlText = rstm.getSqlText();
			if (StringUtil.hasValue(sqlText))
			{
				String  divId       = "sqlText_" + sqlText.hashCode();
				boolean showAtStart = false;

				String htmlContent = ""
						+ "<hr> \n"
						+ "<xmp> \n"
						+ sqlText
						+ "</xmp> \n"
						+ "<hr> \n"
						+ "";
				
				String showHideDiv = createShowHideDiv(divId, showAtStart, "Show/Hide SQL Text that produced this report section...", htmlContent);
				
				sb.append( msOutlookAlternateText(showHideDiv, "Show/Hide SQL Text", "") );
			}
		}

		sb.append("</p>\n");
		sb.append("\n");
		return sb.toString();
	}

	
	/**
	 * Get the CounterModel's DIFF Counters as a SQL Comment, which can be appended to the SQL Used to create report content
	 * 
	 * @param cmName   short cm name
	 * @return a string in the form: <code>-- cm='cmName', possibleDiffColumns='c1,c2,c3' \n</code>
	 */
	public String getCmDiffColumnsAsSqlComment(String cmName)
	{
		if (cmName == null)
			return "";

		String sqlComment = "";
		if (CounterController.hasInstance())
		{
			CountersModel cm = CounterController.getInstance().getCmByName(cmName);
			if (cm != null)
			{
				String[] sa = cm.getDiffColumns();
				if (sa != null)
				{
					sqlComment = "-- cm='" + cmName + "', possibleDiffColumns='" + StringUtil.toCommaStr(sa) + "' \n";
				}
			}
		}
		return sqlComment;
	}
	

//	public String createShowHideDiv(String divId, boolean visibleAtStart, String label, String content)
//	{
//		StringBuilder sb = new StringBuilder();
//
//		String visibilityCss = visibleAtStart ? "block" : "none";
//		
//		sb.append("<a href='javascript:;' onclick=\"toggle_visibility('").append(divId).append("');\">").append(label).append("</a> <br>\n");
//		sb.append("<div id='").append(divId).append("' style='display: ").append(visibilityCss).append(";'>\n");
//		sb.append(content);
//		sb.append("</div>\n");
//		sb.append("\n");
//
//		return sb.toString();
//	}

	public String createShowHideDiv(String divId, boolean visibleAtStart, String label, String content)
	{
		StringBuilder sb = new StringBuilder();

		String visibility = visibleAtStart ? "checked" : "unchecked";
		
		// Build the below:
		// <div>
		//     <input type='checkbox' id='DIVID' checked/>
		//     <label for='DIVID'>LABEL GOES HERE</label>
		//     <div class='hide-show'>CONTENT GOES HERE</div>
		// </div>

		sb.append("\n");
		sb.append("<div>\n");
		sb.append("    <input type='checkbox' id='").append(divId).append("' ").append(visibility).append("/> \n");
		sb.append("    <label for='").append(divId).append("'>").append(label).append("</label> \n");
		sb.append("    <div class='hide-show'>\n").append(content).append("\n    </div> \n");
		sb.append("</div>\n");
		sb.append("\n");

		return sb.toString();
	}

	/**
	 * This adds conditional text NOT available if you are using Microsoft Outlook Mail client as a reader.
	 * 
	 * @param contentForOtherReaders   Content for other readers than Microsoft Outlook
	 * @param name                     Typical "name" of the section which will be suppressed for MS Outlook
	 * @param alternateMsoText         Alternate text for Microsoft Outlook... if NULL; A default text will be used
	 * @return
	 */
	public String msOutlookAlternateText(String contentForOtherReaders, String name, String alternateMsoText)
	{
		StringBuilder sb = new StringBuilder();
		
		if (alternateMsoText == null)
		{
			sb.append("\n");
			sb.append("<!--[if mso]>\n");
			sb.append("You are using Microsoft Outlook to read this...<br>\n");
			sb.append("'<i>").append(name).append("</i>' is NOT presented for Microsoft Outlook, <b>open the link at the top to see the '<i>").append(name).append("</i>'...</b><br>\n");
			sb.append("<![endif]-->\n");
		}
		else
		{
			sb.append("\n");
			sb.append("<!--[if mso]>\n");
			sb.append(alternateMsoText).append("<br>\n");
			sb.append("<![endif]-->\n");
		}
		
		sb.append("<!--[if !mso]><!-- -->\n");
		if (contentForOtherReaders != null)
		{
			sb.append(contentForOtherReaders);

			if ( ! contentForOtherReaders.endsWith("\n") )
				sb.append("\n");
		}
		sb.append("<!--<![endif]-->\n");
		
		return sb.toString();
	}

	/**
	 * Calculate "Duration", and set that to a String column in the form "%HH:%MM:%SS"
	 * @param rstm
	 * @param startColName      Start col
	 * @param stopColName       End col
	 * @param durationColName   Name of the column we want to set
	 */
	public void setDurationColumn(ResultSetTableModel rstm, String startColName, String stopColName, String durationColName)
	{
		int pos_StartEntry = rstm.findColumn(startColName);
		int pos_StopEntry  = rstm.findColumn(stopColName);
		int pos_Duration   = rstm.findColumn(durationColName);

		if (pos_StartEntry >= 0 && pos_StopEntry >= 0 && pos_Duration >= 0)
		{
			for (int r=0; r<rstm.getRowCount(); r++)
			{
				Timestamp FirstEntry = rstm.getValueAsTimestamp(r, pos_StartEntry);
				Timestamp LastEntry  = rstm.getValueAsTimestamp(r, pos_StopEntry);

				if (FirstEntry != null && LastEntry != null)
				{
					long durationInMs = LastEntry.getTime() - FirstEntry.getTime();
					String durationStr = TimeUtils.msToTimeStr("%HH:%MM:%SS", durationInMs);
					rstm.setValueAtWithOverride(durationStr, r, pos_Duration);
				}
			}
		}
	}

	

	/**
	 * Get values from PCS table <code>MonSessionParams</code> where <code>ParamName</code> is equal to 'offline.sampleTime'
	 * 
	 * @param conn
	 * @return
	 */
	public int getRecordingSampleTime(DbxConnection conn)
	{
		String val = getRecordingSessionParameter(conn, null, "offline.sampleTime");
		
		return StringUtil.parseInt(val, -1);
	}
	
	/**
	 * Get values from PCS table <code>MonSessionParams</code>
	 * 
	 * @param conn
	 * @param type           can be null, then all "types" will be searched, but lat one found will be returned.
	 * @param paramName      Name of the parameter (or content of column <code>ParamName</code>)
	 * 
	 * @return The value as a String, NULL if not found.
	 */
	public String getRecordingSessionParameter(DbxConnection conn, String type, String paramName)
	{
		String whereType = "";
		if (StringUtil.hasValue(type))
			whereType = "  and [Type] = '" + type + "' \n";
		
		String tabName = "MonSessionParams";
		String sql = ""
			    + "select [Type], [ParamName], [ParamValue] \n"
			    + "from ["+tabName+"] \n"
			    + "where [ParamName] = '" + paramName + "' \n"
			    + whereType
			    + "";

		String paramValue = null;
		
		sql = conn.quotifySqlString(sql);
		try ( Statement stmnt = conn.createStatement() )
		{
			// Unlimited execution time
			stmnt.setQueryTimeout(0);
			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				while(rs.next())
				{
//					String type = rs.getString(1);
//					String name = rs.getString(2);
					String val  = rs.getString(3);

					paramValue = val;
				}
			}
		}
		catch(SQLException ex)
		{
			_logger.warn("Problems getting values from '"+tabName+"': " + ex);
		}

		return paramValue;
	}
	

	public String getDbxCentralLinkWithDescForGraphs(boolean addSpaceBefore, String description, String... graphList)
	{
		StringBuilder sb = new StringBuilder();

		if (addSpaceBefore)
		{
			sb.append("<br><br><br>\n");
			sb.append("<hr>\n");
		}
		sb.append("<p>\n");
		sb.append(description).append(" <br>\n");
		sb.append("<a href='").append(getDbxCentralLinkForGraphs(graphList)).append("'>Link to DbxCentral Graphs</a> which is a bit more dynamic than static images.\n");
		sb.append("</p>\n");
		
		return sb.toString();
	}
	public String getDbxCentralLinkForGraphs(String... graphList)
	{
		String dbxcLink = getReportingInstance().getDbxCentralBaseUrl();
		RecordingInfo recordingInfo = getReportingInstance().getInstanceRecordingInfo();

		String startTime = recordingInfo.getStartTime();
		String endTime   = recordingInfo.getEndTime();
		String srvName   = getReportingInstance().getServerName();
//		String graphList = "CmSummary_aaCpuGraph,CmEngines_cpuSum,CmEngines_cpuEng,CmSysLoad_EngineRunQLengthGraph";

		if (StringUtil.hasValue(startTime)) startTime = startTime.substring(0, "YYYY-MM-DD".length()) + " 00:00";
		if (StringUtil.hasValue(endTime))   endTime   = endTime  .substring(0, "YYYY-MM-DD".length()) + " 23:59";
		
		String dbxCentralLink = dbxcLink + "/graph.html"
				+ "?subscribe=false"
				+ "&cs=dark"
				+ "&gcols=1"
				+ "&sessionName=" + srvName 
				+ "&startTime="   + startTime 
				+ "&endTime="     + endTime 
				+ "&graphList="   + StringUtil.toCommaStr(graphList, ",") // note: normal StringUtil.toCommaStr(graphList)... makes separator as ", " (with a space after the comma)
				+ "";
		
		return dbxCentralLink;
	}
	
	/**
	 * Simple wrapper method to create a ReportChartObject
	 * @param conn
	 * @param cmName
	 * @param graphName
	 * @param maxValue
	 * @param skipNames
	 * @param graphTitle
	 * 
	 * @return This will always returns a ReportChartObject object
	 */
	public ReportChartObject createChart(DbxConnection conn, String cmName, String graphName, int maxValue, String skipNames, String graphTitle)
	{
		return new ReportChartObject(this, conn, cmName, graphName, maxValue, skipNames, graphTitle);
	}
}
