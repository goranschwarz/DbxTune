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
package com.asetune.pcs.report.content.postgres;

import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.ReportChartObject;
import com.asetune.pcs.report.content.ase.AseAbstract;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

public class PostgresDbSize extends AseAbstract
{
//	private static Logger _logger = Logger.getLogger(AseCpuUsageOverview.class);

	public PostgresDbSize(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public String getMsgAsText()
	{
		return "Text Report is not implemented";
	}

	@Override
	public String getMsgAsHtml()
	{
		StringBuilder sb = new StringBuilder();

		sb.append(getDbxCentralLinkWithDescForGraphs(false, "Below are Size of each Database during the day.",
				"CmPgDatabase_DbSizeMb"
				));

		sb.append(_CmPgDatabase_DbSizeMb.getHtmlContent(null, null));


		if (hasProblem())
			sb.append("<pre>").append(getProblem()).append("</pre> \n");

		sb.append("\n<br>");

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
	public void create(DbxConnection conn, String srvName, Configuration conf)
	{
		_CmPgDatabase_DbSizeMb = createChart(conn, "CmPgDatabase", "DbSizeMb", -1, null, "DB Size in MB (Databases)");
	}

	private ReportChartObject _CmPgDatabase_DbSizeMb;
}
