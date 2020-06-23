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
package com.asetune.pcs.report.content.postgres;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.text.StrBuilder;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.ReportChartObject;
import com.asetune.pcs.report.content.ase.AseAbstract;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

public class PostgresDbSize extends AseAbstract
{
//	private static Logger _logger = Logger.getLogger(PostgresDbSize.class);

	private ResultSetTableModel _shortRstm;

	public PostgresDbSize(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public String getMessageText()
	{
		StringBuilder sb = new StringBuilder();

		// Get a description of this section, and column names
		sb.append(getSectionDescriptionHtml(_shortRstm, true));

		// Last sample Database Size info
		sb.append("Row Count: ").append(_shortRstm.getRowCount()).append("<br>\n");
		sb.append(_shortRstm.toHtmlTableString("sortable"));
		
//		int sumNumBackends = _shortRstm.getSumValueAsInteger("numbackends");
//		int sumDbSizeMb    = _shortRstm.getSumValueAsInteger("dbsize_mb");

		int sumDbSizeMb    = 0;
		int sumNumBackends = 0;
		for (int r=0; r<_shortRstm.getRowCount(); r++)
		{
			sumDbSizeMb    += _shortRstm.getValueAsInteger(r, "dbsize_mb");
			sumNumBackends += _shortRstm.getValueAsInteger(r, "numbackends");
		}
//		sb.append("<br>\n");
//		sb.append("<b>Total Size in MB:  </b>").append(sumDbSizeMb).append("<br>\n");
//		sb.append("<b>Total numbackends: </b>").append(sumNumBackends).append("<br>\n");
//		sb.append("<br>\n");
		LinkedHashMap<String, Object> summaryMap = new LinkedHashMap<>();
		summaryMap.put("Total Size in MB",  sumDbSizeMb);
		summaryMap.put("Total numbackends", sumNumBackends);
		
		sb.append("<br>\n");
		sb.append(StringUtil.toHtmlTable(summaryMap));
		sb.append("<br>\n");

		
		sb.append(getDbxCentralLinkWithDescForGraphs(false, "Below are Size of each Database during the day.",
				"CmPgDatabase_DbSizeMb"
				));

		sb.append(_CmPgDatabase_DbSizeMb.getHtmlContent(null, null));

		return sb.toString();
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

		// Describe the table
		setSectionDescription(_shortRstm);
		
		_CmPgDatabase_DbSizeMb = createChart(conn, "CmPgDatabase", "DbSizeMb", -1, null, "DB Size in MB (Databases)");
	}

	private ReportChartObject _CmPgDatabase_DbSizeMb;

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
