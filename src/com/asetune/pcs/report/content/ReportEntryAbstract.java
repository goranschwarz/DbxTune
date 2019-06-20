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
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.sql.conn.DbxConnection;
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
		ResultSetTableModel rstm = new ResultSetTableModel(rs, name, sql);

		// Set Timestamp format to "yyyy-MM-dd HH:mm:ss"
		rstm.setToStringTimestampFormat_YMD_HMS();
		
		return rstm;
	}


	public ResultSetTableModel executeQuery(DbxConnection conn, String sql, boolean onErrorCreateEmptyRstm, String name)
	{
		// transform all "[" and "]" to DBMS Vendor Quoted Identifier Chars 
		sql = conn.quotifySqlString(sql);

		try ( Statement stmnt = conn.createStatement() )
		{
			// Unlimited execution time
			stmnt.setQueryTimeout(0);
			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				ResultSetTableModel rstm = createResultSetTableModel(rs, name, sql);
				
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
			sb.append("<br>\n");
			sb.append("Column(s) description:\n");
			sb.append("<ul>\n");

			List<String> colNames = rstm.getColumnNames();
			for (String colName : colNames)
			{
				String colDesc = rstm.getColumnDescription(colName);
				
				sb.append("  <li><b>").append(colName).append("</b> - ").append(colDesc).append("</li>\n");
			}

			sb.append("</ul>\n");
		}

		if (addSqlText)
		{
			String sqlText = rstm.getSqlText();
			if (StringUtil.hasValue(sqlText))
			{
				String divId = "sqlText_" + sqlText.hashCode();

				sb.append("<br>\n");
				sb.append("<a href='javascript:;' onclick=\"toggle_visibility('" + divId + "');\">Show/Hide SQL Text ...</a> <br>\n");
				sb.append("<div id='" + divId + "' style='display: none'>\n");
				sb.append("<hr>\n");
				sb.append("<xmp>\n");
				sb.append(sqlText);
				sb.append("</xmp>\n");
				sb.append("<hr>\n");
				sb.append("</div>\n");
			}
		}

		sb.append("</p>\n\n");
		return sb.toString();
	}
}
