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

import org.apache.log4j.Logger;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.DailySummaryReportFactory;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

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

			String htmlContent = "<ul> \n";
			for (String colName : rstm.getColumnNames())
				htmlContent += "  <li><b>" + colName + "</b> - " + rstm.getColumnDescription(colName) + "</li> \n";
			htmlContent += "</ul> \n";

			sb.append( createShowHideDiv(divId, showAtStart, "Show/Hide Column(s) description:", htmlContent) );
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
				
				sb.append( createShowHideDiv(divId, showAtStart, "Show/Hide SQL Text ...", htmlContent) );
			}
		}

		sb.append("</p>\n");
		sb.append("\n");
		return sb.toString();
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
	 * @param name                     Typical "name" of the section which will be suppressed for MS Outlook
	 * @param contentForOtherReaders   Content for other readers than Microsoft Outlook
	 * @return
	 */
	public String msOutlookAlternateText(String name, String contentForOtherReaders)
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append("\n");
		sb.append("<!--[if mso]>\n");
		sb.append("You are using Microsoft Outlook to read this...<br>\n");
		sb.append(name).append(" is NOT presented for Microsoft Outlook, <b>open the link at the top to see the ").append(name).append("...</b><br>\n");
		sb.append("<![endif]-->\n");
		
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

}
