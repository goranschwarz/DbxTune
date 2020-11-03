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
package com.asetune.pcs.report.content.sqlserver;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

public class SqlServerUnusedIndexes
extends SqlServerAbstract
{
//	private static Logger _logger = Logger.getLogger(SqlServerUnusedIndexes.class);

	private ResultSetTableModel _shortRstm;

	public SqlServerUnusedIndexes(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public void writeMessageText(Writer sb)
	throws IOException
	{
		// Get a description of this section, and column names
		sb.append(getSectionDescriptionHtml(_shortRstm, true));

		// Last sample Database Size info
		sb.append("Row Count: " + _shortRstm.getRowCount() + "<br>\n");
		sb.append(toHtmlTable(_shortRstm));
	}

//	@Override
//	public String getMessageText()
//	{
//		StringBuilder sb = new StringBuilder();
//
//		// Get a description of this section, and column names
//		sb.append(getSectionDescriptionHtml(_shortRstm, true));
//
//		// Last sample Database Size info
//		sb.append("Row Count: ").append(_shortRstm.getRowCount()).append("<br>\n");
////		sb.append(_shortRstm.toHtmlTableString("sortable"));
//		sb.append(toHtmlTable(_shortRstm));
//
//		return sb.toString();
//	}

	@Override
	public String getSubject()
	{
		return "Top Unused Indexes (origin: CmIndexUnused)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmIndexUnused_abs" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		int topRows = localConf.getIntProperty(this.getClass().getSimpleName()+".top", 20);

		String sql = ""
			    + "select top " + topRows + " * \n"
			    + "from [CmIndexUnused_abs] x \n"
			    + "where [SessionSampleTime] = (select max([SessionSampleTime]) from [CmIndexUnused_abs]) \n"
			    + "order by [user_updates] desc \n"
			    + "";
		
		_shortRstm = executeQuery(conn, sql, true, "CmIndexUnused_abs");

		// Describe the table
		setSectionDescription(_shortRstm);
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
				"Information from last collector sample from the table <code>CmIndexUnused_abs</code><br>" +
				"");
	}
}

