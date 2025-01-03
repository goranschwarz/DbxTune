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
package com.dbxtune.pcs.report.content.sqlserver;

import java.io.IOException;
import java.io.Writer;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

public class SqlServerMissingIndexes
extends SqlServerAbstract
{
//	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private ResultSetTableModel _shortRstm;

	public SqlServerMissingIndexes(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public boolean hasMinimalMessageText()
	{
		return false;
	}

	@Override
	public boolean hasShortMessageText()
	{
		return false;
	}

//	@Override
//	public void writeShortMessageText(Writer w)
//	throws IOException
//	{
//	}

	@Override
	public void writeMessageText(Writer sb, MessageType messageType)
	throws IOException
	{
		// Get a description of this section, and column names
		sb.append(getSectionDescriptionHtml(_shortRstm, true));

		// Last sample Database Size info
//		sb.append("Row Count: " + _shortRstm.getRowCount() + "<br>\n");
		sb.append("Row Count: " + _shortRstm.getRowCount() + "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");
		sb.append(toHtmlTable(_shortRstm));
	}

	@Override
	public String getSubject()
	{
		return "Top Missing Indexes (origin: CmIndexMissing)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmIndexMissing_abs" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
//		int topRows = localConf.getIntProperty(this.getClass().getSimpleName()+".top", 20);
		int topRows = getTopRows();

		String sql = ""
			    + "select top " + topRows + " * \n"
			    + "from [CmIndexMissing_abs] x \n"
			    + "where [SessionSampleTime] = (select max([SessionSampleTime]) from [CmIndexMissing_abs]) \n"
			    + "order by [Impact] desc \n"
			    + "";
		
		_shortRstm = executeQuery(conn, sql, true, "CmIndexMissing_abs");

		// Highlight sort column
		_shortRstm.setHighlightSortColumns("Impact");

		// Remove some columns which we don't really need
		_shortRstm.removeColumnNoCase("SessionStartTime");
		_shortRstm.removeColumnNoCase("SessionSampleTime");
//		_shortRstm.removeColumnNoCase("CmSampleTime");
		_shortRstm.removeColumnNoCase("CmSampleMs");
		_shortRstm.removeColumnNoCase("CmNewDiffRateRow");  // This was changed into "CmRowState"
		_shortRstm.removeColumnNoCase("CmRowState");
		
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
				"Information from last collector sample from the table <code>CmIndexMissing_abs</code><br>" +
				"");
	}
}

