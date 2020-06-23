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
package com.asetune.pcs.report.content.ase;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.ReportChartObject;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

public class AseStatementCacheUsageOverview extends AseAbstract
{
//	private static Logger _logger = Logger.getLogger(AseStatementCacheUsageOverview.class);

	ResultSetTableModel _cfg;

	public AseStatementCacheUsageOverview(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public String getMessageText()
	{
		StringBuilder sb = new StringBuilder();

		sb.append(getDbxCentralLinkWithDescForGraphs(false, "Below are some Graphs/Charts regarding the Statement Cache.",
				"CmStatementCache_HitRatePctGraph",
				"CmStatementCache_RequestPerSecGraph",
				"CmSqlStatement_SqlStmnt"
				));

		if (_cfg != null && _cfg.getRowCount() > 0)
			sb.append(_cfg.toHtmlTableString("sortable"));

		sb.append(_CmStatementCache_HitRatePctGraph   .getHtmlContent(null, null));
		sb.append(_CmStatementCache_RequestPerSecGraph.getHtmlContent(null, null));
		sb.append(_CmSqlStatement_SqlStmnt            .getHtmlContent(null, "The above graph is a bit more than just from the Statement Cache, and the origin is from monSysStatement"));

		return sb.toString();
	}

	@Override
	public String getSubject()
	{
		return "Statement Cache Overview (origin: CmStatementCache)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		// Get 'statement cache size' from saved configuration 
		String sql = "select [ConfigName], [ConfigValue], [UsedMemory] \n"
				+ "from [MonSessionDbmsConfig] \n"
				+ "where [SessionStartTime] = (select max([SessionStartTime]) from [MonSessionDbmsConfig]) \n"
				+ "  and [ConfigName] = 'statement cache size' \n";
		_cfg = 	executeQuery(conn, sql, false, "StatementCacheSize");

		
		int maxValue = 100;
		_CmStatementCache_HitRatePctGraph    = createChart(conn, "CmStatementCache", "HitRatePctGraph",    maxValue, null, "Statement Cache Hit Rate, in Percent (Cache->Statement Cache)");
		_CmStatementCache_RequestPerSecGraph = createChart(conn, "CmStatementCache", "RequestPerSecGraph",       -1, null, "Number of Requests from the Statement Cache, per Second (Cache->Statement Cache)");

		_CmSqlStatement_SqlStmnt             = createChart(conn, "CmSqlStatement",   "SqlStmnt",                 -1, null, "SQL Statements Executed per Sec (Object/Access->SQL Statements)");
	}

	private ReportChartObject _CmStatementCache_HitRatePctGraph;
	private ReportChartObject _CmStatementCache_RequestPerSecGraph;

	private ReportChartObject _CmSqlStatement_SqlStmnt;
}
