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
package com.dbxtune.pcs.report.content.postgres;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashMap;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportChart;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

public class PostgresDbSize
extends PostgresAbstract
{
//	private static Logger _logger = Logger.getLogger(PostgresDbSize.class);

	private ResultSetTableModel _shortRstm;

	public PostgresDbSize(DailySummaryReportAbstract reportingInstance)
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
		// Get a description of this section, and column names
		sb.append(getSectionDescriptionHtml(_shortRstm, true));

		// Last sample Database Size info
		sb.append("Row Count: " + _shortRstm.getRowCount() + "<br>\n");
		sb.append(toHtmlTable(_shortRstm));
		
		int sumDbSizeMb    = 0;
		int sumNumBackends = 0;
		for (int r=0; r<_shortRstm.getRowCount(); r++)
		{
			sumDbSizeMb    += _shortRstm.getValueAsInteger(r, "dbsize_mb");
			sumNumBackends += _shortRstm.getValueAsInteger(r, "numbackends");
		}

		LinkedHashMap<String, Object> summaryMap = new LinkedHashMap<>();
		summaryMap.put("Total Size in MB",  sumDbSizeMb);
		summaryMap.put("Total numbackends", sumNumBackends);
		
		sb.append("<br>\n");
		sb.append(StringUtil.toHtmlTable(summaryMap));
		sb.append("<br>\n");

		
		sb.append(getDbxCentralLinkWithDescForGraphs(false, "Below are Size of each Database during the day.",
				"CmPgDatabase_DbSizeMb"
				));

		_CmPgDatabase_DbSizeMb.writeHtmlContent(sb, null, null);
	}

	@Override
	public String getSubject()
	{
		return "Database Size in MB (origin: CmPgDatabase / pg_stat_databases + pg_database_size(datname))";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmPgDatabase_abs" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		String sql = ""
			    + "select \n"
			    + "     [datname] \n"
			    + "    ,[dbsize_mb] \n"
			    + "    ,[numbackends] \n"
			    + "    ,[srv_cfg_max_connections] \n"
			    + "    ,[datid] \n"
			    + "from [CmPgDatabase_abs] \n"
			    + "where [SessionSampleTime] = (select max([SessionSampleTime]) from [CmPgDatabase_abs]) \n"
			    + "order by [dbsize_mb] desc \n"
			    + "";
		
		_shortRstm = executeQuery(conn, sql, true, "CmOpenDatabases_abs");

		// Highlight sort column
		_shortRstm.setHighlightSortColumns("dbsize_mb");

		// Describe the table
		setSectionDescription(_shortRstm);
		
		String schema = getReportingInstance().getDbmsSchemaName();
		
		_CmPgDatabase_DbSizeMb = createTsLineChart(conn, schema, "CmPgDatabase", "DbSizeMb", -1, true, null, "DB Size in MB (Databases)");
	}

	private IReportChart _CmPgDatabase_DbSizeMb;

	/**
	 * Set descriptions for the table, and the columns
	 */
	private void setSectionDescription(ResultSetTableModel rstm)
	{
		if (rstm == null)
			return;
		
		// Section description
		rstm.setDescription(
				"Information from last collector sample from the table <code>CmPgDatabase_abs</code><br>" +
				"This will show you sizes of all databases and there last Usage. <br>" +
				"");
	}
}
